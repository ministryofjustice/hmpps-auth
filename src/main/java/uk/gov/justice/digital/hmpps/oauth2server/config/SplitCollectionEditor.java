package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.propertyeditors.CustomCollectionEditor;

import java.util.Collection;

public class SplitCollectionEditor extends CustomCollectionEditor {

    private final Class<? extends Collection> collectionType;
    private final String splitRegex;

    public SplitCollectionEditor(final Class<? extends Collection> collectionType, final String splitRegex) {
        super(collectionType, true);
        this.collectionType = collectionType;
        this.splitRegex = splitRegex;
    }

    @Override
    public void setAsText(final String text) throws IllegalArgumentException {
        if (text.isEmpty()) {
            super.setValue(super.createCollection(this.collectionType, 0));
        } else {
            super.setValue(text.split(splitRegex));
        }
    }
}
