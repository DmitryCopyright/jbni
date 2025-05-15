package com.tuneit.jackalope.integration.babelnet.DTO;

public class CreateSenseRequestDto {
    private String named_id;
    private String canonical_wordform;
    private Object editor;

    public void setNamed_id(String named_id) {
        this.named_id = named_id;
    }

    public void setCanonical_wordform(String canonical_wordform) {
        this.canonical_wordform = canonical_wordform;
    }

    public void setEditor(Object editor) {
        this.editor = editor;
    }

    @Override
    public String toString() {
        return "CreateSenseRequestDto{" +
                "named_id='" + named_id + '\'' +
                ", canonical_wordform='" + canonical_wordform + '\'' +
                ", editor=" + editor +
                '}';
    }
}

