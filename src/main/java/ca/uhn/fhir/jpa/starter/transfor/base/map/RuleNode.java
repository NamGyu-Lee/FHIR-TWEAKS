package ca.uhn.fhir.jpa.starter.transfor.base.map;


import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.util.RuleUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RuleNode {
	@NonNull
	String rule;

	@NonNull
	RuleType ruleType;

	@NonNull
	TransactionType transactionType;

	@NonNull
	String targetElementNm;

	String sourceReferenceNm;

	boolean isIdentifierNode;

	boolean isArrayNode;

	int level;

	List<RuleNode> children;

	RuleNode parent;

	public RuleNode(RuleNode parentRuleNode, String rule, int level, boolean isIdentifierNode, boolean isArrayNode) {
		this.parent = parentRuleNode;
		this.rule = rule;
		ruleType = RuleUtils.classifyRuleType(rule);
		if(ruleType.equals(RuleType.TRANS)){
			transactionType = RuleUtils.classifyTransactionType(rule);
			sourceReferenceNm = RuleUtils.getSourceReferenceName(rule);
			targetElementNm = RuleUtils.getTargetElementName(rule);
		}else{
			transactionType = RuleUtils.classifyTransactionType(rule);
			targetElementNm = rule;
		}
		this.level = level;
		this.children = new ArrayList<>();
		this.isIdentifierNode = isIdentifierNode;
		this.isArrayNode = isArrayNode;
	}

	public void addChild(RuleNode child) {
		this.children.add(child);
	}

	public RuleNode copyNode(){
		RuleNode cpNode = new RuleNode(this.getParent(), this.rule, this.level, this.isIdentifierNode, this.isArrayNode);
		if(cpNode.getRuleType().equals(RuleType.TRANS)){
			cpNode.setTransactionType(RuleUtils.classifyTransactionType(cpNode.getRule()));
			cpNode.setSourceReferenceNm(RuleUtils.getSourceReferenceName(cpNode.getRule()));
			cpNode.setTargetElementNm(RuleUtils.getTargetElementName(cpNode.getRule()));
		}else{
			cpNode.setTransactionType(RuleUtils.classifyTransactionType(cpNode.getRule()));
			cpNode.setTargetElementNm(cpNode.getRule());
		}
		return cpNode;
	}


}
