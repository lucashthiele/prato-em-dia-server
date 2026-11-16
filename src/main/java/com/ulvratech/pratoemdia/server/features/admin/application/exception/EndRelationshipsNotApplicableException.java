package com.ulvratech.pratoemdia.server.features.admin.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

/** {@code endRelationships=true} only makes sense when deactivating a NUTRITIONIST account. */
public class EndRelationshipsNotApplicableException extends CodedApplicationException {
    public EndRelationshipsNotApplicableException() {
        super("END_RELATIONSHIPS_NOT_APPLICABLE");
    }
}
