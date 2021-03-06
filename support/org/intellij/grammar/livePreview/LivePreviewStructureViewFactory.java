package org.intellij.grammar.livePreview;

import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.intellij.grammar.psi.BnfFile;
import org.intellij.grammar.psi.BnfRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.intellij.grammar.generator.ParserGeneratorUtil.getPsiClassPrefix;
import static org.intellij.grammar.generator.ParserGeneratorUtil.getRulePsiClassName;

/**
 * @author gregsh
 */
public class LivePreviewStructureViewFactory implements PsiStructureViewFactory {

  @Nullable
  public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
    if (!(psiFile.getLanguage() instanceof LivePreviewLanguage)) return null;
    return new TreeBasedStructureViewBuilder() {
      @NotNull
      @Override
      public StructureViewModel createStructureViewModel() {
        return new MyModel(psiFile);
      }

      @Override
      public boolean isRootNodeShown() {
        return false;
      }
    };
  }

  private static class MyModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider{
    protected MyModel(@NotNull PsiFile psiFile) {
      super(psiFile, new MyElement(psiFile));
      withSuitableClasses(PsiElement.class);
    }

    @Override
    public boolean shouldEnterElement(Object element) {
      return true;
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
      return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
      return element.getValue() instanceof LeafPsiElement;
    }

  }

  private static class MyElement extends PsiTreeElementBase<PsiElement> implements SortableTreeElement {

    MyElement(PsiElement element) {
      super(element);
    }

    @NotNull
    public Collection<StructureViewTreeElement> getChildrenBase() {
      PsiElement element = getElement();
      if (element == null || element instanceof LeafPsiElement) return Collections.emptyList();
      ArrayList<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();
      for (PsiElement e = element.getFirstChild(); e != null; e = e.getNextSibling()) {
        if (e instanceof PsiWhiteSpace) continue;
        result.add(new MyElement(e));
      }
      return result;
    }

    @Override
    public String getAlphaSortKey() {
      return getPresentableText();
    }

    @NotNull
    @Override
    public String getPresentableText() {
      PsiElement element = getElement();
      ASTNode node = element != null ? element.getNode() : null;
      IElementType elementType = node != null ? node.getElementType() : null;
      if (element instanceof LeafPsiElement) {
        return elementType + "('" + element.getText() + "')";
      }
      else if (element instanceof PsiErrorElement) {
        return "PsiErrorElement: '" + ((PsiErrorElement)element).getErrorDescription() + "'";
      }
      else if (elementType instanceof LivePreviewParser.RuleElementType) {
        BnfRule rule = ((LivePreviewParser.RuleElementType)elementType).rule;
        String prefix = getPsiClassPrefix((BnfFile)rule.getContainingFile());
        String className = getRulePsiClassName(rule, prefix);
        return className + ": '" + StringUtil.first(element.getText(), 30, true) +"'";
      }
      return elementType + "";
    }

    @Nullable
    @Override
    public String getLocationString() {
      return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
      return null;
    }
  }
}
