package net.tntim1.psychic.widget;

/**
 * Controls which popup renderer is shown when a codex widget is clicked.
 */
public enum PopupType {
    /** Scrollable text body.  popupData = plain text, use \n for line-breaks. */
    INFO,
    /** Bulleted list panel.   popupData = newline-separated entries. */
    LIST,
    /** Texture image panel.   popupData = ResourceLocation string. */
    IMAGE,
    /** Custom renderer.       popupData = renderer identifier string. */
    CUSTOM
}
