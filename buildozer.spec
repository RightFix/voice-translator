[app]
title = Talk
package.name = talk
package.domain = org.rightfix.talk

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,json

version = 1.0
requirements = python3,kivy,deep-translator,gtts,pyjnius,plyer,certifi

orientation = portrait
fullscreen = 0

android.permissions = RECORD_AUDIO,INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE
android.api = 31
android.minapi = 23
android.archs = arm64-v8a, armeabi-v7a

android.meta_data = com.google.android.gms.version=@integer/google_play_services_version
android.enable_android_gradle_plugin = True
android.icon.filename = image.png
android.presplash.color = #FFFFFF