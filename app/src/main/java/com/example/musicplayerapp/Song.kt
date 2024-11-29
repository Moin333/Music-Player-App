package com.example.musicplayerapp

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Song : RealmObject {
    @PrimaryKey
    var id: String = ""
    var title: String = ""
    var artist: String = ""
    var urL: String = ""
}