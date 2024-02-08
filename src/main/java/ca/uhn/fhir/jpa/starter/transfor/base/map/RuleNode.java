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

	int level;

	List<RuleNode> children;

	RuleNode parent;

	public RuleNode(RuleNode parentRuleNode, String rule, int level, boolean isIdentifierNode) {
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
	}

	public void addChild(RuleNode child) {
		this.children.add(child);
	}

	public RuleNode copyNode(){
		RuleNode cpNode = new RuleNode(this.getParent(), this.rule, this.level, this.isIdentifierNode);
		return cpNode;
	}


}
