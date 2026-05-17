package com.carlmanning.carlsbrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carlmanning.carlsbrain.domain.model.Bucket

@Entity(tableName = "buckets")
data class BucketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isVault: Boolean = false,
    val isUserCreated: Boolean = false,
    val colorHex: String = "#6750A4",
    val sortOrder: Int = 0
) {
    fun toDomain(): Bucket = Bucket(
        id = id,
        name = name,
        isVault = isVault,
        isUserCreated = isUserCreated,
        colorHex = colorHex,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(bucket: Bucket): BucketEntity = BucketEntity(
            id = bucket.id,
            name = bucket.name,
            isVault = bucket.isVault,
            isUserCreated = bucket.isUserCreated,
            colorHex = bucket.colorHex,
            sortOrder = bucket.sortOrder
        )
    }
}
