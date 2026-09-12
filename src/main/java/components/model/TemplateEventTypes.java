package components.model;

public final class TemplateEventTypes {

    public static final String GROUP_CREATE = "TEMPLATE_GROUP_CREATE";
    public static final String GROUP_TRANSLATE = "TEMPLATE_GROUP_TRANSLATE";
    public static final String GROUP_RESIZE = "TEMPLATE_GROUP_RESIZE";

    private TemplateEventTypes() {
    }

    public static boolean isTeacherOnly(String type) {
        if (type == null) {
            return false;
        }
        return GROUP_TRANSLATE.equals(type) || GROUP_RESIZE.equals(type);
    }
}
