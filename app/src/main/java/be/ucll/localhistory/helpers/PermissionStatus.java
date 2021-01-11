package be.ucll.localhistory.helpers;

public enum PermissionStatus {
    GRANTED,
    DENIED,
    DONT_ASK_OR_NEVER_ASKED, // if never ask again was selected or never asked before
    UNKNOWN,
}
