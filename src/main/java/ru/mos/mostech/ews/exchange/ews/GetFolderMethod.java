/*
DIT
 */
package ru.mos.mostech.ews.exchange.ews;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * EWS GetFolder method.
 */

@Slf4j
public class GetFolderMethod extends EWSMethod {

    /**
     * Get folder method.
     *
     * @param baseShape            base requested shape
     * @param folderId             folder id
     * @param additionalProperties additional requested properties
     */
    public GetFolderMethod(BaseShape baseShape, FolderId folderId, Set<FieldURI> additionalProperties) {
        super("Folder", "GetFolder");
        this.baseShape = baseShape;
        this.folderId = folderId;
        this.additionalProperties = additionalProperties;
    }

}