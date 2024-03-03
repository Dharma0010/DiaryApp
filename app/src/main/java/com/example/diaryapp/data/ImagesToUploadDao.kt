package com.example.diaryapp.data

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diaryapp.data.database.entity.ImageToUpload

interface ImagesToUploadDao {

    @Query("SELECT * FROM images_to_upload_table ORDER BY id ASC")
    suspend fun getAllImages(): List<ImageToUpload>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addImageToUpload(imageToUpload: ImageToUpload)

    @Query("DELETE FROM images_to_upload_table WHERE id=:imageId")
    suspend fun cleanUpImage(imageId: Int)
}