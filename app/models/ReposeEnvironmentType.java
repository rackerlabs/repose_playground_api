package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public enum ReposeEnvironmentType {
    GENERATED_ORIGIN ("Generated origin and repose instances [default]"),
    GENERATED_THIRDPARTIES ("Generated origin, repose, and third party instances"),
    MIXED_THIRD_PARTIES("Generated origin and repose instance and mix of third party instances"),
    SPECIFIED_ORIGIN("Generated repose and specified origin"),
    SPECIFIED_ORIGIN_GENERATED_THIRD_PARTIES("Generated repose and third party instances and specified origin"),
    SPECIFIED_ORIGIN_SPECIFIED_THIRD_PARTIES("Generated repose and specified third parties and origin"),
    SPECIFIED_ORIGIN_MIXED_THIRD_PARTIES("Generated repose, mixed third parties, and specified origin");

    private String explanation;

    ReposeEnvironmentType(String explanation){
        this.explanation = explanation;
    }

}
