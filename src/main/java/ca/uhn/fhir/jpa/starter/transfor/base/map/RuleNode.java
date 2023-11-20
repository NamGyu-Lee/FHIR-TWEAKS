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

	int level;

	List<RuleNode> children;

	RuleNode parent;

	public RuleNode(RuleNode parentRuleNode, String rule, int level) {
		this.parent = parentRuleNode;
		this.rule = rule;
		ruleType = RuleUtils.classifyRuleType(rule);
		if(ruleType.equals(RuleType.TRANS)){
			transactionType = RuleUtils.classifyTransactionType(rule);
			sourceReferenceNm = RuleUtils.getSourceReferenceName(rule);
			targetElementNm = RuleUtils.getTargetElementName(rule);
		}else{
			transactionType = TransactionType.CREATE;
			targetElementNm = rule;
		}
		this.level = level;
		this.children = new ArrayList<>();
	}

	public void addChild(RuleNode child) {
		this.children.add(child);
	}


}
