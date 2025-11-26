package com.flowboard.data.models.crdt

/**
 * Interface for objects that can be synchronized across clients
 */
interface Syncable {
    var synkId: String
    var synkLastModified: Long
}
