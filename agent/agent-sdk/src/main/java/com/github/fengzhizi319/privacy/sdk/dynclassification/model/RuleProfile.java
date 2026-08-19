package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a complete domain rule pack.
 * 定义完整的领域规则包。
 */
public class RuleProfile {
    private String domain;
    private String version = "1.0.0";
    private String description;
    private List<RuleDef> rules = new ArrayList<>();
    private List<DowngradeRuleDef> downgradeRules = new ArrayList<>();
    private List<CompositeRuleDef> compositeRules = new ArrayList<>();
    private Object redactionStrategy;

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<RuleDef> getRules() { return rules; }
    public void setRules(List<RuleDef> rules) { this.rules = rules; }

    public List<DowngradeRuleDef> getDowngradeRules() { return downgradeRules; }
    public void setDowngradeRules(List<DowngradeRuleDef> downgradeRules) { this.downgradeRules = downgradeRules; }

    public List<CompositeRuleDef> getCompositeRules() { return compositeRules; }
    public void setCompositeRules(List<CompositeRuleDef> compositeRules) { this.compositeRules = compositeRules; }

    public Object getRedactionStrategy() { return redactionStrategy; }
    public void setRedactionStrategy(Object redactionStrategy) { this.redactionStrategy = redactionStrategy; }

    /**
     * Creates a default built-in rule profile with common PII detection rules.
     */
    public static RuleProfile createDefault() {
        RuleProfile p = new RuleProfile();
        p.setDomain("default");
        p.setVersion("1.0.0");

        List<RuleDef> rules = new ArrayList<>();

        // ID card rule
        RuleDef idCard = new RuleDef();
        idCard.setId("id-card");
        idCard.setLevel("L4");
        idCard.setCategory("identity");
        idCard.setPriority(100);
        idCard.setMatchLogic("AND");
        List<MatcherDef> idCardMatchers = new ArrayList<>();
        MatcherDef idRegex = new MatcherDef();
        idRegex.setOperator("regex");
        idRegex.getParams().put("pattern", "^\\d{17}[\\dXx]$");
        idCardMatchers.add(idRegex);
        MatcherDef idChecksum = new MatcherDef();
        idChecksum.setOperator("id_card_checksum");
        idCardMatchers.add(idChecksum);
        idCard.setMatchers(idCardMatchers);
        rules.add(idCard);

        // Mobile phone rule
        RuleDef mobile = new RuleDef();
        mobile.setId("mobile-phone");
        mobile.setLevel("L3");
        mobile.setCategory("contact");
        mobile.setPriority(90);
        List<MatcherDef> mobileMatchers = new ArrayList<>();
        MatcherDef mobileRegex = new MatcherDef();
        mobileRegex.setOperator("regex");
        mobileRegex.getParams().put("pattern", "^1[3-9]\\d{9}$");
        mobileMatchers.add(mobileRegex);
        mobile.setMatchers(mobileMatchers);
        rules.add(mobile);

        // Email rule
        RuleDef email = new RuleDef();
        email.setId("email");
        email.setLevel("L3");
        email.setCategory("contact");
        email.setPriority(80);
        List<MatcherDef> emailMatchers = new ArrayList<>();
        MatcherDef emailRegex = new MatcherDef();
        emailRegex.setOperator("regex");
        emailRegex.getParams().put("pattern", "^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
        emailMatchers.add(emailRegex);
        email.setMatchers(emailMatchers);
        rules.add(email);

        // Bank card rule
        RuleDef bankCard = new RuleDef();
        bankCard.setId("bank-card");
        bankCard.setLevel("L4");
        bankCard.setCategory("financial");
        bankCard.setPriority(95);
        bankCard.setMatchLogic("AND");
        List<MatcherDef> bankMatchers = new ArrayList<>();
        MatcherDef bankRegex = new MatcherDef();
        bankRegex.setOperator("regex");
        bankRegex.getParams().put("pattern", "^\\d{16,19}$");
        bankMatchers.add(bankRegex);
        MatcherDef luhn = new MatcherDef();
        luhn.setOperator("luhn_checksum");
        bankMatchers.add(luhn);
        bankCard.setMatchers(bankMatchers);
        rules.add(bankCard);

        // Field name based rules
        RuleDef fieldNameId = new RuleDef();
        fieldNameId.setId("field-name-id");
        fieldNameId.setLevel("L4");
        fieldNameId.setCategory("identity");
        fieldNameId.setPriority(70);
        List<MatcherDef> fieldNameIdMatchers = new ArrayList<>();
        MatcherDef kwId = new MatcherDef();
        kwId.setOperator("keyword_contains");
        kwId.getParams().put("keywords", List.of("id_card", "idcard", "身份证", "sfz"));
        kwId.getParams().put("fields", List.of("name"));
        fieldNameIdMatchers.add(kwId);
        fieldNameId.setMatchers(fieldNameIdMatchers);
        rules.add(fieldNameId);

        RuleDef fieldNamePhone = new RuleDef();
        fieldNamePhone.setId("field-name-phone");
        fieldNamePhone.setLevel("L3");
        fieldNamePhone.setCategory("contact");
        fieldNamePhone.setPriority(60);
        List<MatcherDef> fieldNamePhoneMatchers = new ArrayList<>();
        MatcherDef kwPhone = new MatcherDef();
        kwPhone.setOperator("keyword_contains");
        kwPhone.getParams().put("keywords", List.of("phone", "mobile", "手机", "电话"));
        kwPhone.getParams().put("fields", List.of("name"));
        fieldNamePhoneMatchers.add(kwPhone);
        fieldNamePhone.setMatchers(fieldNamePhoneMatchers);
        rules.add(fieldNamePhone);

        p.setRules(rules);
        return p;
    }
}
