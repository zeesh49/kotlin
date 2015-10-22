/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.idea.formatter.NodeIndentStrategy.strategy;

/**
 * @see Block for good JavaDoc documentation
 */
public class KotlinBlock extends AbstractBlock {
    private static final int KDOC_COMMENT_INDENT = 1;
    private final NodeAlignmentStrategy myAlignmentStrategy;
    private final Indent myIndent;
    private final CodeStyleSettings mySettings;
    private final KotlinSpacingBuilder mySpacingBuilder;

    private List<Block> mySubBlocks;

    private static final TokenSet BINARY_EXPRESSIONS = TokenSet.create(KtNodeTypes.BINARY_EXPRESSION, KtNodeTypes.BINARY_WITH_TYPE, KtNodeTypes.IS_EXPRESSION);
    private static final TokenSet QUALIFIED_OPERATION = TokenSet.create(KtTokens.DOT, KtTokens.SAFE_ACCESS);
    private static final TokenSet ALIGN_FOR_BINARY_OPERATIONS =
            TokenSet.create(KtTokens.MUL, KtTokens.DIV, KtTokens.PERC, KtTokens.PLUS, KtTokens.MINUS, KtTokens.ELVIS, KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ, KtTokens.ANDAND, KtTokens.OROR);

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            KtNodeTypes.BLOCK,
            KtNodeTypes.CLASS_BODY,
            KtNodeTypes.FUNCTION_LITERAL);

    private static final TokenSet KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC,
                                                                 KDocElementTypes.KDOC_SECTION,
                                                                 KDocElementTypes.KDOC_TAG);

    // private static final List<IndentWhitespaceRule>

    public KotlinBlock(
            @NotNull ASTNode node,
            @NotNull NodeAlignmentStrategy alignmentStrategy,
            Indent indent,
            Wrap wrap,
            CodeStyleSettings settings,
            KotlinSpacingBuilder spacingBuilder
    ) {
        super(node, wrap, alignmentStrategy.getAlignment(node));
        myAlignmentStrategy = alignmentStrategy;
        myIndent = indent;
        mySettings = settings;
        mySpacingBuilder = spacingBuilder;
    }

    @Override
    public Indent getIndent() {
        return myIndent;
    }

    @Override
    protected List<Block> buildChildren() {
        if (mySubBlocks == null) {
            List<Block> nodeSubBlocks = buildSubBlocks();

            if (getNode().getElementType() == KtNodeTypes.DOT_QUALIFIED_EXPRESSION || getNode().getElementType() == KtNodeTypes.SAFE_ACCESS_EXPRESSION) {
                int operationBlockIndex = findNodeBlockIndex(nodeSubBlocks, QUALIFIED_OPERATION);
                if (operationBlockIndex != -1) {
                    // Create fake ".something" or "?.something" block here, so child indentation will be
                    // relative to it when it starts from new line (see Indent javadoc).

                    Block operationBlock = nodeSubBlocks.get(operationBlockIndex);
                    SyntheticKotlinBlock operationSynteticBlock =
                            new SyntheticKotlinBlock(
                                    ((ASTBlock) operationBlock).getNode(),
                                    nodeSubBlocks.subList(operationBlockIndex, nodeSubBlocks.size()),
                                    null, operationBlock.getIndent(), null, mySpacingBuilder);

                    nodeSubBlocks = ContainerUtil.addAll(
                            ContainerUtil.newArrayList(nodeSubBlocks.subList(0, operationBlockIndex)),
                            operationSynteticBlock);
                }
            }

            mySubBlocks = nodeSubBlocks;
        }
        return mySubBlocks;
    }

    private List<Block> buildSubBlocks() {
        List<Block> blocks = new ArrayList<Block>();

        NodeAlignmentStrategy childrenAlignmentStrategy = getChildrenAlignmentStrategy();
        WrappingStrategy wrappingStrategy = getWrappingStrategy();

        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();

            if (child.getTextRange().getLength() == 0) continue;

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            blocks.add(buildSubBlock(child, childrenAlignmentStrategy, wrappingStrategy));
        }

        return Collections.unmodifiableList(blocks);
    }

    @NotNull
    private Block buildSubBlock(
            @NotNull ASTNode child,
            NodeAlignmentStrategy alignmentStrategy,
            @NotNull WrappingStrategy wrappingStrategy) {
        Wrap wrap = wrappingStrategy.getWrap(child.getElementType());

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.getElementType() == KtNodeTypes.OPERATION_REFERENCE) {
            ASTNode operationNode = child.getFirstChildNode();
            if (operationNode != null) {
                return new KotlinBlock(operationNode, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
            }
        }

        return new KotlinBlock(child, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
    }

    private static ASTNode getPrevWithoutWhitespace(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && node.getElementType() == TokenType.WHITE_SPACE) {
            node = node.getTreePrev();
        }

        return node;
    }

    private static ASTNode getPrevWithoutWhitespaceAndComments(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && (node.getElementType() == TokenType.WHITE_SPACE || KtTokens.COMMENTS.contains(node.getElementType()))) {
            node = node.getTreePrev();
        }

        return node;
    }

    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return mySpacingBuilder.getSpacing(this, child1, child2);
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        IElementType type = getNode().getElementType();
        if (CODE_BLOCKS.contains(type) ||
            type == KtNodeTypes.WHEN ||
            type == KtNodeTypes.IF ||
            type == KtNodeTypes.FOR ||
            type == KtNodeTypes.WHILE ||
            type == KtNodeTypes.DO_WHILE) {

            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
        else if (type == KtNodeTypes.TRY) {
            // In try - try BLOCK catch BLOCK finally BLOCK
            return new ChildAttributes(Indent.getNoneIndent(), null);
        }
        else if (type == KtNodeTypes.DOT_QUALIFIED_EXPRESSION || type == KtNodeTypes.SAFE_ACCESS_EXPRESSION) {
            return new ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null);
        }
        else if (type == KtNodeTypes.VALUE_PARAMETER_LIST || type == KtNodeTypes.VALUE_ARGUMENT_LIST) {
            // Child index 1 - cursor is after ( - parameter alignment should be recreated
            // Child index 0 - before expression - know nothing about it
            if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < getSubBlocks().size()) {
                Block block = getSubBlocks().get(newChildIndex);
                return new ChildAttributes(block.getIndent(), block.getAlignment());
            }
            return new ChildAttributes(Indent.getContinuationIndent(), null);
        }
        else if (type == KtTokens.DOC_COMMENT) {
            return new ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null);
        }

        if (type == KtNodeTypes.PARENTHESIZED) {
            return super.getChildAttributes(newChildIndex);
        }

        List<Block> blocks = getSubBlocks();
        if (newChildIndex != 0) {
            boolean isIncomplete = newChildIndex < blocks.size() ? blocks.get(newChildIndex - 1).isIncomplete() : isIncomplete();
            if (isIncomplete) {
                return super.getChildAttributes(newChildIndex);
            }
        }

        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @NotNull
    private static WrappingStrategy getWrappingStrategyForItemList(int wrapType, @NotNull final IElementType itemType) {
        final Wrap itemWrap = Wrap.createWrap(wrapType, false);
        return new WrappingStrategy() {
            @Nullable
            @Override
            public Wrap getWrap(@NotNull IElementType childElementType) {
                return childElementType == itemType ? itemWrap : null;
            }
        };
    }

    @NotNull
    private WrappingStrategy getWrappingStrategy() {
        CommonCodeStyleSettings commonSettings = mySettings.getCommonSettings(KotlinLanguage.INSTANCE);
        IElementType elementType = myNode.getElementType();

        if (elementType == KtNodeTypes.VALUE_ARGUMENT_LIST) {
            return getWrappingStrategyForItemList(commonSettings.CALL_PARAMETERS_WRAP, KtNodeTypes.VALUE_ARGUMENT);
        }
        if (elementType == KtNodeTypes.VALUE_PARAMETER_LIST) {
            IElementType parentElementType = myNode.getTreeParent().getElementType();
            if (parentElementType == KtNodeTypes.FUN || parentElementType == KtNodeTypes.CLASS) {
                return getWrappingStrategyForItemList(commonSettings.METHOD_PARAMETERS_WRAP, KtNodeTypes.VALUE_PARAMETER);
            }
        }

        return WrappingStrategy.NoWrapping.INSTANCE$;
    }

    private NodeAlignmentStrategy getChildrenAlignmentStrategy() {
        final CommonCodeStyleSettings jetCommonSettings = mySettings.getCommonSettings(KotlinLanguage.INSTANCE);
        KotlinCodeStyleSettings jetSettings = mySettings.getCustomSettings(KotlinCodeStyleSettings.class);

        // Redefine list of strategies for some special elements
        IElementType parentType = myNode.getElementType();
        if (parentType == KtNodeTypes.VALUE_PARAMETER_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS, KtNodeTypes.VALUE_PARAMETER, KtTokens.COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, KtTokens.LPAR, KtTokens.RPAR);
        }
        else if (parentType == KtNodeTypes.VALUE_ARGUMENT_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, KtNodeTypes.VALUE_ARGUMENT, KtTokens.COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, KtTokens.LPAR, KtTokens.RPAR);
        }
        else if (parentType == KtNodeTypes.WHEN) {
            return getAlignmentForCaseBranch(jetSettings.ALIGN_IN_COLUMNS_CASE_BRANCH);
        }
        else if (parentType == KtNodeTypes.WHEN_ENTRY) {
            // Propagate when alignment for ->
            return myAlignmentStrategy;
        }
        else if (BINARY_EXPRESSIONS.contains(parentType) && ALIGN_FOR_BINARY_OPERATIONS.contains(getOperationType(getNode()))) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())));
        }
        else if (parentType == KtNodeTypes.DELEGATION_SPECIFIER_LIST || parentType == KtNodeTypes.INITIALIZER_LIST) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, getAlignment())));
        }
        else if (parentType == KtNodeTypes.PARENTHESIZED) {
            return new NodeAlignmentStrategy() {
                Alignment bracketsAlignment = jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION ? Alignment.createAlignment() : null;

                @Nullable
                @Override
                public Alignment getAlignment(@NotNull ASTNode childNode) {
                    IElementType childNodeType = childNode.getElementType();
                    ASTNode prev = getPrevWithoutWhitespace(childNode);

                    if ((prev != null && prev.getElementType() == TokenType.ERROR_ELEMENT) || childNodeType == TokenType.ERROR_ELEMENT) {
                        return bracketsAlignment;
                    }

                    if (childNodeType == KtTokens.LPAR || childNodeType == KtTokens.RPAR) {
                        return bracketsAlignment;
                    }

                    return null;
                }
            };
        }

        return NodeAlignmentStrategy.getNullStrategy();
    }

    private static NodeAlignmentStrategy getAlignmentForChildInParenthesis(
            boolean shouldAlignChild, final IElementType parameter, final IElementType delimiter,
            boolean shouldAlignParenthesis, final IElementType openBracket, final IElementType closeBracket
    ) {
        final Alignment parameterAlignment = shouldAlignChild ? Alignment.createAlignment() : null;
        final Alignment bracketsAlignment = shouldAlignParenthesis ? Alignment.createAlignment() : null;

        return new NodeAlignmentStrategy() {
            @Override
            public Alignment getAlignment(@NotNull ASTNode node) {
                IElementType childNodeType = node.getElementType();

                ASTNode prev = getPrevWithoutWhitespace(node);
                if ((prev != null && prev.getElementType() == TokenType.ERROR_ELEMENT) || childNodeType == TokenType.ERROR_ELEMENT) {
                    // Prefer align to parameters on incomplete code (case of line break after comma, when next parameters is absent)
                    return parameterAlignment;
                }

                if (childNodeType == openBracket || childNodeType == closeBracket) {
                    return bracketsAlignment;
                }

                if (childNodeType == parameter || childNodeType == delimiter) {
                    return parameterAlignment;
                }

                return null;
            }
        };
    }

    private static NodeAlignmentStrategy getAlignmentForCaseBranch(boolean shouldAlignInColumns) {
        if (shouldAlignInColumns) {
            return NodeAlignmentStrategy.fromTypes(
                    AlignmentStrategy.createAlignmentPerTypeStrategy(Arrays.asList((IElementType) KtTokens.ARROW), KtNodeTypes.WHEN_ENTRY, true));
        }
        else {
            return NodeAlignmentStrategy.getNullStrategy();
        }
    }

    private static final NodeIndentStrategy[] INDENT_RULES = new NodeIndentStrategy[] {
            strategy("No indent for braces in blocks")
                    .in(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)
                    .forType(KtTokens.RBRACE, KtTokens.LBRACE)
                    .set(Indent.getNoneIndent()),

            strategy("Indent for block content")
                    .in(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)
                    .notForType(KtTokens.RBRACE, KtTokens.LBRACE, KtNodeTypes.BLOCK)
                    .set(Indent.getNormalIndent(false)),

            strategy("Indent for property accessors")
                    .in(KtNodeTypes.PROPERTY)
                    .forType(KtNodeTypes.PROPERTY_ACCESSOR)
                    .set(Indent.getNormalIndent()),

            strategy("For a single statement in 'for'")
                    .in(KtNodeTypes.BODY)
                    .notForType(KtNodeTypes.BLOCK)
                    .set(Indent.getNormalIndent()),

            strategy("For the entry in when")
                    .forType(KtNodeTypes.WHEN_ENTRY)
                    .set(Indent.getNormalIndent()),

            strategy("For single statement in THEN and ELSE")
                    .in(KtNodeTypes.THEN, KtNodeTypes.ELSE)
                    .notForType(KtNodeTypes.BLOCK)
                    .set(Indent.getNormalIndent()),

            strategy("Indent for parts")
                    .in(KtNodeTypes.PROPERTY, KtNodeTypes.FUN, KtNodeTypes.MULTI_VARIABLE_DECLARATION)
                    .notForType(KtNodeTypes.BLOCK, KtTokens.FUN_KEYWORD, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
                    .set(Indent.getContinuationWithoutFirstIndent()),

            strategy("Chained calls")
                    .in(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("Delegation list")
                    .in(KtNodeTypes.DELEGATION_SPECIFIER_LIST, KtNodeTypes.INITIALIZER_LIST)
                    .set(Indent.getContinuationIndent(false)),

            strategy("Indices")
                    .in(KtNodeTypes.INDICES)
                    .set(Indent.getContinuationIndent(false)),

            strategy("Binary expressions")
                    .in(BINARY_EXPRESSIONS)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("Parenthesized expression")
                    .in(KtNodeTypes.PARENTHESIZED)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("KDoc comment indent")
                    .in(KDOC_CONTENT)
                    .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
                    .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

            strategy("Block in when entry")
                    .in(KtNodeTypes.WHEN_ENTRY)
                    .notForType(KtNodeTypes.BLOCK, KtNodeTypes.WHEN_CONDITION_EXPRESSION, KtNodeTypes.WHEN_CONDITION_IN_RANGE, KtNodeTypes.WHEN_CONDITION_IS_PATTERN, KtTokens.ELSE_KEYWORD, KtTokens.ARROW)
                    .set(Indent.getNormalIndent()),
    };

    @Nullable
    protected static Indent createChildIndent(@NotNull ASTNode child) {
        ASTNode childParent = child.getTreeParent();
        IElementType childType = child.getElementType();

        // SCRIPT: Avoid indenting script top BLOCK contents
        if (childParent != null && childParent.getTreeParent() != null) {
            if (childParent.getElementType() == KtNodeTypes.BLOCK && childParent.getTreeParent().getElementType() == KtNodeTypes.SCRIPT) {
                return Indent.getNoneIndent();
            }
        }

        // do not indent child after heading comments inside declaration
        if (childParent != null && childParent.getPsi() instanceof KtDeclaration) {
            ASTNode prev = getPrevWithoutWhitespace(child);
            if (prev != null && KtTokens.COMMENTS.contains(prev.getElementType()) && getPrevWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent();
            }
        }

        for (NodeIndentStrategy strategy : INDENT_RULES) {
            Indent indent = strategy.getIndent(child);
            if (indent != null) {
                return indent;
            }
        }

        // TODO: Try to rewrite other rules to declarative style
        if (childParent != null) {
            IElementType parentType = childParent.getElementType();

            if (parentType == KtNodeTypes.VALUE_PARAMETER_LIST || parentType == KtNodeTypes.VALUE_ARGUMENT_LIST) {
                ASTNode prev = getPrevWithoutWhitespace(child);
                if (childType == KtTokens.RPAR && (prev == null || prev.getElementType() != TokenType.ERROR_ELEMENT)) {
                    return Indent.getNoneIndent();
                }

                return Indent.getContinuationWithoutFirstIndent();
            }

            if (parentType == KtNodeTypes.TYPE_PARAMETER_LIST || parentType == KtNodeTypes.TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent();
            }
        }

        return Indent.getNoneIndent();
    }

    @Nullable
    private static Alignment createAlignment(boolean alignOption, @Nullable Alignment defaultAlignment) {
        return alignOption ? createAlignmentOrDefault(null, defaultAlignment) : defaultAlignment;
    }

    @Nullable
    private static Alignment createAlignmentOrDefault(@Nullable Alignment base, @Nullable Alignment defaultAlignment) {
        if (defaultAlignment == null) {
            return base == null ? Alignment.createAlignment() : Alignment.createChildAlignment(base);
        }
        return defaultAlignment;
    }

    private static int findNodeBlockIndex(List<Block> blocks, final TokenSet tokenSet) {
        return ContainerUtil.indexOf(blocks, new Condition<Block>() {
            @Override
            public boolean value(Block block) {
                if (!(block instanceof ASTBlock)) return false;

                ASTNode node = ((ASTBlock) block).getNode();
                return node != null && tokenSet.contains(node.getElementType());
            }
        });
    }

    @Nullable
    private static IElementType getOperationType(ASTNode node) {
        ASTNode operationNode = node.findChildByType(KtNodeTypes.OPERATION_REFERENCE);
        return operationNode != null ? operationNode.getFirstChildNode().getElementType() : null;
    }
}
