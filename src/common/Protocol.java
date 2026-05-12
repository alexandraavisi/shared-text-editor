package common;

public class Protocol {

    // CLIENT -> SERVER
    public static final String CONNECT    = "CONNECT";
    public static final String LIST       = "LIST";
    public static final String VIEW       = "VIEW";
    public static final String EDIT       = "EDIT";
    public static final String SAVE       = "SAVE";
    public static final String RELEASE    = "RELEASE";
    public static final String DISCONNECT = "DISCONNECT";

    // SERVER -> CLIENT
    public static final String OK           = "OK";
    public static final String ERROR        = "ERROR";
    public static final String FILE_LIST    = "FILE_LIST";
    public static final String FILE_CONTENT = "FILE_CONTENT";

    // NOTIFICARI SERVER -> CLIENT
    public static final String NOTIFY_EDIT    = "NOTIFY_EDIT";
    public static final String NOTIFY_RELEASE = "NOTIFY_RELEASE";
    public static final String NOTIFY_SAVE    = "NOTIFY_SAVE";
    public static final String NOTIFY_ADD     = "NOTIFY_ADD";
    public static final String NOTIFY_DELETE  = "NOTIFY_DELETE";

    // STATUS FISIER
    public static final String FREE    = "FREE";
    public static final String EDITING = "EDITING";

    public static final String SEP       = "|";
    public static final String NO_EDITOR = "-";
}