I started receiving this message when I build my project

> Could not find com.arthenica:ffmpeg-kit-full:6.0-2.
  Searched in the following locations:
    - https://dl.google.com/dl/android/maven2/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
    - https://repo.maven.apache.org/maven2/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
    - https://jcenter.bintray.com/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
    - https://jitpack.io/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
    - https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
    - https://oss.sonatype.org/content/repositories/snapshots/com/arthenica/ffmpeg-kit-full/6.0-2/ffmpeg-kit-full-6.0-2.pom
  Required by:
      project :presentation > project :domain
These are the project's repositories

allprojects {     
   repositories {
      google()
      mavenCentral()
      jcenter()
      maven { url 'https://jitpack.io' }
      maven { url "https://oss.sonatype.org/content/repositories/snapshots" }     
   } 
}
And this is the dependency I'm adding

    implementation(libs.arthenica.ffmpeg.full)
This is my libs.version.toml

ffmpeg = "6.0-2"
arthenica-ffmpeg-full = {  group = "com.arthenica", name = "ffmpeg-kit-full", version.ref = "ffmpeg" }
AFAICS in the project's github page, it will not be maintained anymore ffmpeg-kit is archived on April 21, 2025

In the short term, do you know any other repository that still servers this dependency?

On the other hand, do you know any other project to replace Arthenica ffmpeg-kit-full dependency?

Thank you very much
androidffmpegandroid-ffmpeg
Share
Improve this question
Follow
asked Apr 30 at 10:47
gabocalero's user avatar
gabocalero
57388 silver badges1616 bronze badges
1
"do you know any other project to replace Arthenica ffmpeg-kit-full dependency?" -- asking for recommendations for off-site resources is considered to be off-topic. "do you know any other repository that still servers this dependency?" -- you might want to read the Medium post linked to from the project README, if you have not done so already. – 
CommonsWare
 Commented Apr 30 at 10:52
1
As a short-term solution, you can add AAR files to the project. – 
sdex
 Commented Apr 30 at 11:16
I was using ffmpeg-kit for (a legacy feature) creating a thumbnail from a video. I am replacing ffmpeg-kit dependency by Coil, which covers the same feature in a simpler way – 
gabocalero
 Commented Apr 30 at 13:58
Hi, were you able to find a solution? – 
Usama
 Commented May 5 at 15:15
Add a comment
6 Answers
Sorted by:

 
3


Their post suggests building FFmpegKit locally and using the binaries created for our applications.

So, first of all, if you're developing an Android app, you'll need to clone their project, go to that directory, and run the following commands

export ANDROID_SDK_ROOT=<Android SDK Path>
export ANDROID_NDK_ROOT=<Android NDK Path>
./android.sh
Note that ndk should be the same as latest versions FFmpegKit was compiled - you've got all information here. Also, if you're using a LTS version make sure you add --lts flag to ./android.sh command - (./android-sh --lts).

Then, go to …./ffmpeg-kit/prebuilt/bundle-android-aar/ (or …./ffmpeg-kit/prebuilt/bundle-android-aar-lts in case you're using LTS version) and copy the .aar file to your project (your_project/app/libs).

Finally,

dependencies{
    implementation(files("libs/ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-java9:0.2.1")
}
implementation("com.arthenica:smart-exception-java9:0.2.1") was added because FFmpegKit depends on this library to print stack traces - and you'll get an exception if you don't add that. You've got more information here and here.

Edited: For more details check out this blogpost.
Share
Improve this answer
Follow
edited May 22 at 14:50
answered May 5 at 16:18
mariaiffonseca's user avatar
mariaiffonseca
3944 bronze badges
As it’s currently written, your answer is unclear. Please edit to add additional details that will help others understand how this addresses the question asked. You can find more information on how to write good answers in the help center. – 
Community
Bot
 Commented May 6 at 1:30
please use implementation("com.arthenica:smart-exception-java:0.2.1") instead. – 
Mihuilk
 Commented Jul 9 at 8:03
Add a comment
 
3


Maybe this information comes to late but if its can help anyone i found the solution, this solution is maybe temporal because is a compiled binary of a last version of FfmpegKit in his full version, this works in video and audio with full caracteristics.

First download the .aar file in this link:
https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/

later go to your project structure yourProject/app/libs if you dont have a folder named libs in your app folder only create this folder and place the .aar file into this.

next you need delete your dependece of this:

implementation 'com.arthenica:ffmpeg-kit-full-gpl:6.0-2'
and change to this:

implementation(files("libs/ffmpeg-kit-full-gpl-6.0-2.LTS.aar"))
implementation("com.arthenica:smart-exception-java9:0.2.1")
and thats all your project will continue work without changes.
Share
Improve this answer
Follow
answered Jun 13 at 5:57
Braian Stiven Peña Morales's user avatar
Braian Stiven Peña Morales
4911 bronze badge
is there any min-gpl aar file anywhere? – 
Aishik kirtaniya
 Commented Jun 21 at 19:48
Yes, in the same website you can choose any version of this library link – 
Braian Stiven Peña Morales
Commented Jun 26 at 5:34
seems like the aar wont even work as they have dependency to the ffmpeg library – 
Aishik kirtaniya
Commented Jun 27 at 17:00
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/arthenica/smartexception/java/Exceptions; at com.arthenica.ffmpegkit.FFmpegKitConfig.<clinit>(FFmpegKitConfig.java:134) at com.arthenica.ffmpegkit.FFmpegKit.executeAsync(FFmpegKit.java:207) – 
Aishik kirtaniya
 Commented Jun 27 at 17:04
1
@BraianStivenPeñaMorales Important note for others reading: com.arthenica:smart-exception-java9:0.2.1 and com.arthenica:smart-exception-java:0.2.1 are not the same! I had to switch from the java9:... version in the original answer, to the java:... version in this comment, before it worked in my project. (presumably project-dependent) – 
Venryx
 Commented Jul 13 at 12:27
Show 2 more comments
 
2


I also ran into this problem.

The solution is to download files from the website: https://github.com/DucLQ92/ffmpeg-kit-audio/tree/main/com/arthenica/ffmpeg-kit-audio/6.0-2

Next, place the aar file in the app/libs folder

Add it to build.gradle:

dependencies {
     implementation(files("libs/ffmpeg-kit-audio-6.0-2.aar"))
     implementation 'com.arthenica:smart-exception-java:0.2.1'
} 
Assemble the project
Share
Improve this answer
Follow
edited Jun 4 at 13:56
answered Jun 4 at 8:52
Arty Morris's user avatar
Arty Morris
11933 bronze badges
Add a comment
 
1


I tried to follow @mariaiffonseca, but I get an error every time when it tries to build the library I get the following error message: Creating Android archive under prebuilt: failed . Moreover, follow below some log messages:

> Task :ffmpeg-kit-android-lib:compileReleaseJavaWithJavac FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':ffmpeg-kit-android-lib:compileReleaseJavaWithJavac'.
> Could not resolve all files for configuration ':ffmpeg-kit-android-lib:androidJdkImage'.
   > Failed to transform core-for-system-modules.jar to match attributes {artifactType=_internal_android_jdk_image, org.gradle.libraryelements=jar, org.gradle.usage=java-runtime}.
      > Execution failed for JdkImageTransform: /Users/rca/Library/Android//platforms/android-33/core-for-system-modules.jar.
         > Error while executing process /Users/rca/Library/Java/JavaVirtualMachines/corretto-21.0.5/Contents/Home{--module-path /Users/rca/.gradle/caches/transforms-3/ef45e0af4d32a105d29fb530a1beed17/transformed/output/temp/jmod --add-modules java.base --/Users/rca/.gradle/caches/transforms-3/ef45e0af4d32a105d29fb530a1beed17/transformed/output/jdkImage --disable-plugin system-modules}

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
Share
Improve this answer
Follow
edited May 22 at 1:59
answered May 15 at 17:18
Renan Costa Alencar's user avatar
Renan Costa Alencar
3166 bronze badges
Add a comment
 
0


I've solved the issue with


    implementation(files("libs/ffmpeg-kit-min-gpl-6.0-2.aar"))
    implementation(files("libs/smart-exception-java-0.2.1.jar"))

'smart-exception-java' also should be downloaded.

The files are in here: https://artifactory.appodeal.com/appodeal-public/com/arthenica/
Share
Improve this answer
Follow
answered Jul 10 at 7:39
Dekdori's user avatar
Dekdori
2133 bronze badges
Add a comment
 
-1


try this:

implementation("com.arthenica:ffmpeg-kit-full:6.0-2.LTS")
or this:

implementation("com.arthenica:mobile-ffmpeg-min:4.4.LTS")