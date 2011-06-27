package org.antlr.v4.codegen;

import org.antlr.v4.analysis.AnalysisPipeline;
import org.antlr.v4.codegen.model.*;
import org.antlr.v4.codegen.model.ast.AddLeaf;
import org.antlr.v4.misc.*;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.*;

import java.util.List;

/** */
public class ParserFactory extends OutputModelFactory {
	public ParserFactory(CodeGenerator gen) { super(gen); }

	public OutputModelObject buildOutputModel() {
		return new ParserFile(this, gen.getRecognizerFileName());
	}

	public CodeBlock epsilon() { return new CodeBlock(this); }

	public CodeBlock alternative(List<SrcOp> elems) { return new CodeBlock(this, elems); }

	public SrcOp action(GrammarAST ast) { return new Action(this, ast); }

	public SrcOp forcedAction(GrammarAST ast) { return new ForcedAction(this, ast); }

	public SrcOp sempred(GrammarAST ast) { return new SemPred(this, ast); }

	public List<SrcOp> ruleRef(GrammarAST ID, GrammarAST label, GrammarAST args) {
		InvokeRule r = new InvokeRule(this, ID, label);
		AddToLabelList a = null;
		if ( label!=null && label.parent.getType()==ANTLRParser.PLUS_ASSIGN ) {
			a = new AddToLabelList(this, gen.target.getListLabel(label.getText()), r);
		}
		return Utils.list(r, a);
	}

	public List<SrcOp> tokenRef(GrammarAST ID, GrammarAST label, GrammarAST args) {
		MatchToken matchOp = new MatchToken(this, (TerminalAST) ID, label);
		AddToLabelList labelOp = null;
		if ( label!=null && label.parent.getType()==ANTLRParser.PLUS_ASSIGN ) {
			String listLabel = gen.target.getListLabel(label.getText());
			labelOp = new AddToLabelList(this, listLabel, matchOp);
		}
		SrcOp treeOp = null;
		if ( g.hasASTOption() ) {
			treeOp = new AddLeaf(this, ID, matchOp);
		}
		return Utils.list(matchOp, labelOp, treeOp);
	}

	public List<SrcOp> stringRef(GrammarAST ID, GrammarAST label) {
		return tokenRef(ID, label, null);
	}

	public Choice getChoiceBlock(BlockAST blkAST, List<CodeBlock> alts) {
		int decision = ((DecisionState)blkAST.atnState).decision;
		if ( AnalysisPipeline.disjoint(g.decisionLOOK.get(decision)) ) {
			return getLL1ChoiceBlock(blkAST, alts);
		}
		else {
			return getLLStarChoiceBlock(blkAST, alts);
		}
	}

	public Choice getEBNFBlock(GrammarAST ebnfRoot, List<CodeBlock> alts) {
		int decision;
		if ( ebnfRoot.getType()==ANTLRParser.POSITIVE_CLOSURE ) {
			decision = ((PlusBlockStartState)ebnfRoot.atnState).loopBackState.decision;
		}
		else if ( ebnfRoot.getType()==ANTLRParser.CLOSURE ) {
			decision = ((BlockStartState)ebnfRoot.atnState).decision;
		}
		else {
			decision = ((DecisionState)ebnfRoot.atnState).decision;
		}
		if ( AnalysisPipeline.disjoint(g.decisionLOOK.get(decision)) ) {
			return getLL1EBNFBlock(ebnfRoot, alts);
		}
		else {
			return getLLStarEBNFBlock(ebnfRoot, alts);
		}
	}

	public Choice getLL1ChoiceBlock(BlockAST blkAST, List<CodeBlock> alts) {
		return new LL1AltBlock(this, blkAST, alts);
	}

	public Choice getLLStarChoiceBlock(BlockAST blkAST, List<CodeBlock> alts) {
		return new AltBlock(this, blkAST, alts);
	}

	public Choice getLL1EBNFBlock(GrammarAST ebnfRoot, List<CodeBlock> alts) {
		int ebnf = 0;
		if ( ebnfRoot!=null ) ebnf = ebnfRoot.getType();
		Choice c = null;
		switch ( ebnf ) {
			case ANTLRParser.OPTIONAL :
				if ( alts.size()==1 ) c = new LL1OptionalBlockSingleAlt(this, ebnfRoot, alts);
				else c = new LL1OptionalBlock(this, ebnfRoot, alts);
				break;
			case ANTLRParser.CLOSURE :
				if ( alts.size()==1 ) c = new LL1StarBlockSingleAlt(this, ebnfRoot, alts);
				else c = new LL1StarBlock(this, ebnfRoot, alts);
				break;
			case ANTLRParser.POSITIVE_CLOSURE :
				if ( alts.size()==1 ) c = new LL1PlusBlockSingleAlt(this, ebnfRoot, alts);
				else c = new LL1PlusBlock(this, ebnfRoot, alts);
				break;
		}
		return c;
	}

	public Choice getLLStarEBNFBlock(GrammarAST ebnfRoot, List<CodeBlock> alts) {
		int ebnf = 0;
		if ( ebnfRoot!=null ) ebnf = ebnfRoot.getType();
		Choice c = null;
		switch ( ebnf ) {
			case ANTLRParser.OPTIONAL :
				c = new OptionalBlock(this, ebnfRoot, alts);
				break;
			case ANTLRParser.CLOSURE :
				c = new StarBlock(this, ebnfRoot, alts);
				break;
			case ANTLRParser.POSITIVE_CLOSURE :
				c = new PlusBlock(this, ebnfRoot, alts);
				break;
		}
		return c;
	}

	public SrcOp getLL1Test(IntervalSet look, GrammarAST blkAST) {
		return new TestSetInline(this, blkAST, look);
	}
}