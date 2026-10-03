# 三字经 Android TV App 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建全离线的 Android TV《三字经》亲子共学应用：大字拼音逐句朗读、释义典故面板、学习进度追踪。

**Architecture:** 单 `:app` 模块，Compose for TV + 单 Activity + Navigation Compose；UDF（ViewModel + StateFlow）；内容 JSON 打包 assets 启动时解析一次（内存只读），进度存 Room，设置存 DataStore，音频 MediaPlayer 播 assets MP3；手动 DI（AppContainer）。

**Tech Stack:** Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.10.2 / androidx.tv:tv-material:1.0.1 / Room 2.6.1(kapt) / DataStore 1.1.1 / kotlinx-serialization / Robolectric 4.14.1（JVM 单测）/ Python 脚本（内容、音频、图标、字体子集）。

**规格文档:** `docs/superpowers/specs/2026-10-03-sanzijing-tv-app-design.md`

**与规格的差异说明:** 规格中章节数量为预期值（"以实际整理为准"）。按通行版全文实际整理结果为：教育之要 14 句、蒙学伦理 39 句、经典导读 34 句、历史纲鉴 60 句、勤学典故 33 句、劝学结语 12 句，共 **192 个六字单元**（u001–u192）。规格其余内容不变。

---

## 全局约定（每个任务都适用）

- **工作目录**: `D:/develop/src/rakor/threecharactorscripture`（下称 `$P`）。Shell 是 Windows Git Bash。
- **构建前置环境**（每个运行 gradle 的步骤都要先执行）:
  ```bash
  cd "D:/develop/src/rakor/threecharactorscripture"
  export JAVA_HOME="/d/develop/soft/Android Studio/jbr"
  export PATH="$JAVA_HOME/bin:$PATH"
  export http_proxy=http://127.0.0.1:9999 https_proxy=http://127.0.0.1:9999
  ```
- **Python 前置**（运行 tools/ 脚本的步骤）:
  ```bash
  cd "D:/develop/src/rakor/threecharactorscripture/tools"
  export http_proxy=http://127.0.0.1:9999 https_proxy=http://127.0.0.1:9999
  ```
  Windows 下 python 命令可能是 `python` 或 `py`，先用 `python --version || py --version` 确认，以下统一写 `python`。
- **测试命令**: `./gradlew testDebugUnitTest --tests "类名"`；**打包**: `./gradlew assembleDebug`，产物在 `app/build/outputs/apk/debug/app-debug.apk`。
- 所有 Kotlin 源码包根为 `com.rakor.sanzijing`。
- 提交信息用中文，feat/test/docs/chore 前缀。

## 文件结构（总览）

```
settings.gradle.kts  build.gradle.kts  gradle.properties  local.properties(不入库)
app/build.gradle.kts  app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/res/values/{strings.xml,themes.xml}
app/src/main/res/font/wenkai_subset.ttf            (Task 9 生成)
app/src/main/res/drawable-nodpi/banner.png         (Task 14 生成)
app/src/main/res/mipmap-*/ic_launcher.png          (Task 14 生成)
app/src/main/assets/content/sanzijing.json         (Task 3 生成)
app/src/main/assets/audio/u001..u192.mp3           (Task 14 生成)
app/src/main/java/com/rakor/sanzijing/
  SanzijingApp.kt            # Application + AppContainer（手动 DI）
  MainActivity.kt
  ui/navigation/NavGraph.kt
  ui/theme/{Color.kt,Type.kt,Theme.kt}
  ui/home/{HomeScreen.kt,HomeViewModel.kt}
  ui/reader/{ReaderScreen.kt,ReaderViewModel.kt,AnnotationPanel.kt,ActionPanel.kt}
  ui/report/{ReportScreen.kt,ReportViewModel.kt}
  ui/settings/{SettingsScreen.kt,SettingsViewModel.kt}
  data/content/{Models.kt,ContentRepository.kt}
  data/db/{Entities.kt,Daos.kt,AppDatabase.kt}
  data/prefs/SettingsRepository.kt
  data/audio/AudioPlayer.kt
  data/progress/ProgressRepository.kt
app/src/main/java/com/rakor/sanzijing/ui/common/{ToastHost.kt,PinyinPhrase.kt}
app/src/test/java/com/rakor/sanzijing/
  TestFiles.kt
  data/ContentIntegrityTest.kt
  data/ProgressRepositoryTest.kt
  data/SettingsRepositoryTest.kt
  ui/ReaderViewModelTest.kt
  ui/HomeViewModelTest.kt
app/src/androidTest/java/com/rakor/sanzijing/AppSmokeTest.kt
tools/
  sanzijing_raw.txt        # 192 单元原始文本（本计划内含全文）
  gen_content.py           # raw + annotations.json → sanzijing.json
  annotations.json         # 注释/译文/典故（按规范撰写，全量 192 条）
  PINYIN_OVERRIDES         # 并入 gen_content.py
  gen_audio.py             # edge-tts 生成 uXXX.mp3
  subset_font.py           # LXGW 文楷子集 → res/font
  gen_icons.py             # banner + 启动图标
docs/superpowers/plans/2026-10-03-sanzijing-tv-app.md   # 本计划
```

---

### Task 1: 项目脚手架与可运行空壳

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`(不入库), `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/java/com/rakor/sanzijing/{SanzijingApp.kt,MainActivity.kt}`, `app/src/main/res/drawable-nodpi/banner.png`(占位,Task14 替换), `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`(占位)
- 改 `.gitignore`（已有）确认含 `local.properties`、`build/`、`.gradle/`

- [ ] **Step 1: 下载并解压 Gradle，生成 wrapper**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export http_proxy=http://127.0.0.1:9999 https_proxy=http://127.0.0.1:9999
curl -L -o tools/gradle-8.10.2-bin.zip https://services.gradle.org/distributions/gradle-8.10.2-bin.zip
mkdir -p tools && unzip -q -o tools/gradle-8.10.2-bin.zip -d tools
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version   # 应输出 17 或 21
tools/gradle-8.10.2/bin/gradle wrapper --gradle-version 8.10.2
ls gradlew gradle/wrapper/gradle-wrapper.jar   # 确认生成
```

- [ ] **Step 2: 写入构建文件**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "sanzijing"
include(":app")
```

`build.gradle.kts`（根）:
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.java.home=D:/develop/soft/Android Studio/jbr
android.useAndroidX=true
android.nonTransitiveRClass=true
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=9999
systemProp.http.nonProxyHosts=localhost|127.0.0.1
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=9999
```

`local.properties`（已被 .gitignore 排除）:
```properties
sdk.dir=D:/develop/androidsdk
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.rakor.sanzijing"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rakor.sanzijing"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += "mp3" }   // MediaPlayer openFd 要求 assets 音频不压缩
    testOptions {
        unitTests {
            isIncludeAndroidResources = true   // Robolectric
            all { t ->
                t.systemProperty("http.proxyHost", "127.0.0.1")
                t.systemProperty("http.proxyPort", "9999")
                t.systemProperty("https.proxyHost", "127.0.0.1")
                t.systemProperty("https.proxyPort", "9999")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

`app/proguard-rules.pro`: 空文件即可（一行注释 `# 三字经`）。

- [ ] **Step 3: Manifest 与基础资源**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:name="android.software.leanback" android:required="true" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    <application
        android:name=".SanzijingApp"
        android:allowBackup="true"
        android:banner="@drawable/banner"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Sanzijing">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">三字经</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.Sanzijing" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

占位图（保证 Task 1 可编译；Task 14 用脚本替换）——用 Python 生成 1x1 再拉伸不合适，直接生成纯色小图:
```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python -c "from PIL import Image; import os; os.makedirs('../app/src/main/res/drawable-nodpi',exist_ok=True); Image.new('RGB',(640,360),(255,248,236)).save('../app/src/main/res/drawable-nodpi/banner.png'); [os.makedirs(f'../app/src/main/res/mipmap-{d}',exist_ok=True) or Image.new('RGB',(s,s),(255,138,101)).save(f'../app/src/main/res/mipmap-{d}/ic_launcher.png') for d,s in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]]"
```

`app/src/main/java/com/rakor/sanzijing/SanzijingApp.kt`（AppContainer 本任务先建骨架，后续任务往里加字段）:
```kotlin
package com.rakor.sanzijing

import android.app.Application
import android.content.Context

class AppContainer(private val appContext: Context)

class SanzijingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

`app/src/main/java/com/rakor/sanzijing/MainActivity.kt`:
```kotlin
package com.rakor.sanzijing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("三字经")
            }
        }
    }
}
```

- [ ] **Step 4: 首次构建（会下载 AGP/依赖，需数分钟）**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/app-debug.apk` 存在。若依赖解析失败，检查代理是否可达（`curl -x http://127.0.0.1:9999 -I https://dl.google.com`）。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "chore: Android TV 项目脚手架（Compose TV/Room/DataStore 可构建空壳）"
```

---

### Task 2: 《三字经》通行版全文原始文本（192 单元）

**Files:**
- Create: `tools/sanzijing_raw.txt`

格式约定：`#chapter <id> <标题>` 开启章节；单元行 `uXXX|三字，三字`。

- [ ] **Step 1: 写入全文**

`tools/sanzijing_raw.txt` 内容如下（完整 192 行，逐字照抄）:
```
#chapter ch01 教育之要
u001|人之初，性本善
u002|性相近，习相远
u003|苟不教，性乃迁
u004|教之道，贵以专
u005|昔孟母，择邻处
u006|子不学，断机杼
u007|窦燕山，有义方
u008|教五子，名俱扬
u009|养不教，父之过
u010|教不严，师之惰
u011|子不学，非所宜
u012|幼不学，老何为
u013|玉不琢，不成器
u014|人不学，不知义
#chapter ch02 蒙学伦理
u015|为人子，方少时
u016|亲师友，习礼仪
u017|香九龄，能温席
u018|孝于亲，所当执
u019|融四岁，能让梨
u020|弟于长，宜先知
u021|首孝悌，次见闻
u022|知某数，识某文
u023|一而十，十而百
u024|百而千，千而万
u025|三才者，天地人
u026|三光者，日月星
u027|三纲者，君臣义
u028|父子亲，夫妇顺
u029|曰春夏，曰秋冬
u030|此四时，运不穷
u031|曰南北，曰西东
u032|此四方，应乎中
u033|曰水火，木金土
u034|此五行，本乎数
u035|曰仁义，礼智信
u036|此五常，不容紊
u037|稻粱菽，麦黍稷
u038|此六谷，人所食
u039|马牛羊，鸡犬豕
u040|此六畜，人所饲
u041|曰喜怒，曰哀惧
u042|爱恶欲，七情具
u043|匏土革，木石金
u044|丝与竹，乃八音
u045|高曾祖，父而身
u046|身而子，子而孙
u047|自子孙，至玄曾
u048|乃九族，人之伦
u049|父子恩，夫妇从
u050|兄则友，弟则恭
u051|长幼序，友与朋
u052|君则敬，臣则忠
u053|此十义，人所同
#chapter ch03 经典导读
u054|凡训蒙，须讲究
u055|详训诂，明句读
u056|为学者，必有初
u057|小学终，至四书
u058|论语者，二十篇
u059|群弟子，记善言
u060|孟子者，七篇止
u061|讲道德，说仁义
u062|作中庸，子思笔
u063|中不偏，庸不易
u064|作大学，乃曾子
u065|自修齐，至平治
u066|孝经通，四书熟
u067|如六经，始可读
u068|诗书易，礼春秋
u069|号六经，当讲求
u070|有连山，有归藏
u071|有周易，三易详
u072|有典谟，有训诰
u073|有誓命，书之奥
u074|我周公，作周礼
u075|著六官，存治体
u076|大小戴，注礼记
u077|述圣言，礼乐备
u078|曰国风，曰雅颂
u079|号四诗，当讽咏
u080|诗既亡，春秋作
u081|寓褒贬，别善恶
u082|三传者，有公羊
u083|有左氏，有谷梁
u084|经既明，方读子
u085|撮其要，记其事
u086|五子者，有荀扬
u087|文中子，及老庄
#chapter ch04 历史纲鉴
u088|经子通，读诸史
u089|考世系，知终始
u090|自羲农，至黄帝
u091|号三皇，居上世
u092|唐有虞，号二帝
u093|相揖逊，称盛世
u094|夏有禹，商有汤
u095|周文武，称三王
u096|夏传子，家天下
u097|四百载，迁夏社
u098|汤伐夏，国号商
u099|六百载，至纣亡
u100|周武王，始诛纣
u101|八百载，最长久
u102|周辙东，王纲坠
u103|逞干戈，尚游说
u104|始春秋，终战国
u105|五霸强，七雄出
u106|嬴秦氏，始兼并
u107|传二世，楚汉争
u108|高祖兴，汉业建
u109|至孝平，王莽篡
u110|光武兴，为东汉
u111|四百年，终于献
u112|魏蜀吴，争汉鼎
u113|号三国，迄两晋
u114|宋齐继，梁陈承
u115|为南朝，都金陵
u116|北元魏，分东西
u117|宇文周，与高齐
u118|迨至隋，一土宇
u119|不再传，失统绪
u120|唐高祖，起义师
u121|除隋乱，创国基
u122|二十传，三百载
u123|梁灭之，国乃改
u124|梁唐晋，及汉周
u125|称五代，皆有由
u126|炎宋兴，受周禅
u127|十八传，南北混
u128|辽与金，帝号纷
u129|灭金后，宋犹存
u130|至元兴，金绪歇
u131|有宋世，一同灭
u132|并中国，兼戎狄
u133|九十年，国祚废
u134|明太祖，久亲师
u135|传建文，方四祀
u136|迁北京，永乐嗣
u137|迨崇祯，煤山逝
u138|清太祖，膺景命
u139|靖四方，克大定
u140|至宣统，乃大同
u141|十二世，清祚终
u142|廿二史，全在兹
u143|载治乱，知兴衰
u144|读史者，考实录
u145|通古今，若亲目
u146|口而诵，心而惟
u147|朝于斯，夕于斯
#chapter ch05 勤学典故
u148|昔仲尼，师项橐
u149|古圣贤，尚勤学
u150|赵中令，读鲁论
u151|彼既仕，学且勤
u152|披蒲编，削竹简
u153|彼无书，且知勉
u154|头悬梁，锥刺股
u155|彼不教，自勤苦
u156|如囊萤，如映雪
u157|家虽贫，学不辍
u158|如负薪，如挂角
u159|身虽劳，犹苦卓
u160|苏老泉，二十七
u161|始发愤，读书籍
u162|彼既老，犹悔迟
u163|尔小生，宜早思
u164|若梁灏，八十二
u165|对大廷，魁多士
u166|彼既成，众称异
u167|尔小生，宜立志
u168|莹八岁，能咏诗
u169|泌七岁，能赋棋
u170|彼颖悟，人称奇
u171|尔幼学，当效之
u172|蔡文姬，能辨琴
u173|谢道韫，能咏吟
u174|彼女子，且聪敏
u175|尔男子，当自警
u176|唐刘晏，方七岁
u177|举神童，作正字
u178|彼虽幼，身已仕
u179|尔幼学，勉而致
u180|有为者，亦若是
#chapter ch06 劝学结语
u181|犬守夜，鸡司晨
u182|苟不学，曷为人
u183|蚕吐丝，蜂酿蜜
u184|人不学，不如物
u185|幼而学，壮而行
u186|上致君，下泽民
u187|扬名声，显父母
u188|光于前，裕于后
u189|人遗子，金满籝
u190|我教子，惟一经
u191|勤有功，戏无益
u192|戒之哉，宜勉力
```

- [ ] **Step 2: 校验行数与格式**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
grep -c '^u[0-9]' sanzijing_raw.txt          # Expected: 192
grep -c '^#chapter' sanzijing_raw.txt        # Expected: 6
awk -F'|' '/^u[0-9]/{n=split($2,a,"，"); if(n!=2||length(a[1])!=3||length(a[2])!=3) print "BAD:",$0}' sanzijing_raw.txt
# Expected: 无输出
```

- [ ] **Step 3: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add tools/sanzijing_raw.txt && git commit -m "content: 三字经通行版全文 192 单元原始文本（6 章）"
```

---

### Task 3: 内容生成脚本 + 注释译文典故 → sanzijing.json

**Files:**
- Create: `tools/gen_content.py`, `tools/annotations.json`
- Create: `app/src/main/assets/content/sanzijing.json`（脚本生成）

- [ ] **Step 1: 安装 Python 依赖**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python --version || py --version
python -m pip install --quiet pypinyin
```
Expected: pypinyin 安装成功。

- [ ] **Step 2: 写 gen_content.py（含多音字覆盖表）**

`tools/gen_content.py`:
```python
# -*- coding: utf-8 -*-
"""sanzijing_raw.txt + annotations.json -> ../app/src/main/assets/content/sanzijing.json"""
import json, os, sys
from pypinyin import lazy_pinyin, Style

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "sanzijing_raw.txt")
ANN = os.path.join(HERE, "annotations.json")
OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")

# pypinyin 默认读音在《三字经》语境下错误的多音字，按单元整体覆盖（拼音以空格分隔）
PINYIN_OVERRIDES = {
    "u005": ["xī mèng mǔ", "zé lín chǔ"],      # 处 chǔ
    "u020": ["dì yú zhǎng", "yí xiān zhī"],    # 弟通悌 tì，长 zhǎng
    "u075": ["zhù liù guān", "cún zhì tǐ"],    # 著 zhù
    "u103": ["chěng gān gē", "shàng yóu shuì"],# 说 shuì
    "u126": ["yán sòng xīng", "shòu zhōu shàn"],# 禅 shàn
    "u169": ["bì qī suì", "néng fù qí"],       # 泌 bì（李泌）
    "u189": ["rén wèi zǐ", "jīn mǎn yíng"],    # 遗 wèi（赠给）
}

def phrase_pinyin(unit_id, hanzi, idx):
    if unit_id in PINYIN_OVERRIDES:
        return PINYIN_OVERRIDES[unit_id][idx]
    return " ".join(lazy_pinyin(hanzi, style=Style.TONE))

def main():
    chapters = []
    seen_ids = set()
    for raw in open(RAW, encoding="utf-8"):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#chapter"):
            _, cid, title = line.split()
            chapters.append({"id": cid, "title": title, "units": []})
            continue
        uid, text = line.split("|")
        parts = text.split("，")
        assert len(parts) == 2 and all(len(p) == 3 for p in parts), f"bad line: {line}"
        assert uid not in seen_ids, f"duplicate id: {uid}"
        seen_ids.add(uid)
        chapters[-1]["units"].append({
            "id": uid,
            "phrases": [
                {"hanzi": parts[0], "pinyin": phrase_pinyin(uid, parts[0], 0)},
                {"hanzi": parts[1], "pinyin": phrase_pinyin(uid, parts[1], 1)},
            ],
            "annotation": "",
            "translation": "",
        })
    total = sum(len(c["units"]) for c in chapters)
    ann = json.load(open(ANN, encoding="utf-8"))
    missing = [c["units"][i]["id"] for c in chapters for i in range(len(c["units"])) if c["units"][i]["id"] not in ann]
    if missing:
        sys.exit(f"annotations.json 缺少 {len(missing)} 条: {missing[:10]} ...")
    for c in chapters:
        for u in c["units"]:
            a = ann[u["id"]]
            u["annotation"] = a["annotation"]
            u["translation"] = a["translation"]
            if a.get("storyTitle"):
                assert a.get("story"), f"{u['id']} storyTitle 无对应 story"
                u["storyTitle"] = a["storyTitle"]
                u["story"] = a["story"]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump({"version": 1, "chapters": chapters}, open(OUT, "w", encoding="utf-8"),
              ensure_ascii=False, indent=1)
    print(f"OK {len(chapters)} chapters, {total} units -> {os.path.abspath(OUT)}")

if __name__ == "__main__":
    main()
```

- [ ] **Step 3: 撰写 annotations.json（全量 192 条）**

`tools/annotations.json` 顶层结构:
```json
{
  "u001": {"annotation": "……", "translation": "……", "storyTitle": null, "story": null},
  "u005": {"annotation": "……", "translation": "……", "storyTitle": "孟母三迁", "story": "……"}
}
```

**撰写规范（执行者必须遵守）**:

- `annotation`: 逐词注释，30–60 字。格式「字：义。字：义。」覆盖该句关键字词（如"之、初、本"）。
- `translation`: 白话译文，40–90 字，语句面向家长转述给孩子的口吻。
- `storyTitle`/`story`: 成对出现或同时为 null。故事 80–200 字，讲给孩子的语气，结尾一句点题。
- **覆盖要求**: ch04 历史纲鉴（u088–u147）与 ch05 勤学典故（u148–u180）**每个单元必须有典故**；ch01 的 u005（孟母三迁）、u007（窦燕山教子）；ch02 的 u017（黄香温席）、u019（孔融让梨）；其余章节按内容自然选择，目标全文典故覆盖率 ≥ 60%。
- 典故素材（已知对应）: u005 孟母三迁、u006 孟母断机、u007/u008 窦燕山五子登科、u017 黄香温席、u019 孔融让梨、u058 孔子与论语、u060 孟母三迁之外孟子受教、u062/u063 中庸子思、u064 大学曾子、u074 周公制礼、u080 孔子作春秋、u090–u091 炎黄传说、u092–u093 尧舜禅让、u094–u095 大禹治水/商汤/周文王武王、u097 商汤 onwards 各朝代更替故事（每单元选该朝代代表故事：烽火戏诸侯、卧薪尝胆、完璧归赵、破釜沉舟、文景之治、王莽篡汉、光武中兴、三顾茅庐、赤壁之战、淝水之战、孝文帝改革、隋炀帝开运河、玄武门之变/贞观之治、安史之乱、陈桥兵变、靖康之变、岳飞抗金、文天祥、成吉思汗、朱元璋起义、郑和下西洋、闯王进京、康熙平定、宣统退位等按单元对应）、u144 司马迁实录精神、u148 孔子师项橐、u150 赵普半部论语、u152 路温舒编蒲抄书、u154 孙敬悬梁苏秦刺股、u156 车胤囊萤孙康映雪、u158 朱买臣负薪李密挂角、u160 苏洵二十七始发愤、u164 梁灏八十二中状元、u168 祖莹咏诗、u169 李泌赋棋、u172 蔡文姬辨琴、u173 谢道韫咏雪、u176 刘晏正字。

**示例（前 6 条，风格以此为准）**:
```json
{
  "u001": {
    "annotation": "初：刚出生的时候。本：本来、原本。",
    "translation": "每个人刚出生的时候，天性本来都是善良的，就像一张干干净净的白纸。",
    "storyTitle": null, "story": null
  },
  "u002": {
    "annotation": "相：互相、彼此。近：接近、相似。习：后天的习惯与环境影响。",
    "translation": "人天生的本性其实相差不大，只是因为后天环境和习惯不同，才慢慢变得不一样。",
    "storyTitle": null, "story": null
  },
  "u003": {
    "annotation": "苟：如果。教：教育、教导。乃：就、于是。迁：变迁、改变。",
    "translation": "如果从小不好好教育孩子，善良的天性就会随着环境慢慢变坏。",
    "storyTitle": null, "story": null
  },
  "u004": {
    "annotation": "道：方法、道理。贵：可贵、重要的地方。专：专一、持之以恒。",
    "translation": "教育孩子最重要的方法，是专心致志、始终如一，不能三天打鱼两天晒网。",
    "storyTitle": null, "story": null
  },
  "u005": {
    "annotation": "昔：从前。孟母：孟子的母亲。择：挑选。处：居住的地方。",
    "translation": "从前孟子的母亲，为了让孩子有好的成长环境，特意挑选好邻居，几次搬家。",
    "storyTitle": "孟母三迁",
    "story": "孟子小时候家住在墓地旁边，他学着大人哭丧、办丧事。孟母觉得这里不适合孩子住，就搬到了集市旁。孟子又学起商人吆喝杀猪。孟母还是不满意，第三次搬到了学堂旁边。从此，孟子学着读书人行礼、念书，变得彬彬有礼。孟母说：这里才是我孩子该住的地方。后来孟子成为伟大的思想家。这个故事告诉我们：好的环境对孩子的成长非常重要。"
  },
  "u006": {
    "annotation": "子：孟子。断：剪断。机杼：织布机上的梭子和织成的布。",
    "translation": "孟子逃学回家，孟母当着他的面剪断织布机上的布，以此告诫他学习不能半途而废。",
    "storyTitle": "孟母断机",
    "story": "孟子上学后有一天逃学回家。孟母正在织布，她一句话没说，拿起剪刀咔嚓一声把织好的布剪断了。孟子吓坏了。孟母说：你荒废学业，就像我剪断这匹布一样，前面的功夫全白费了。孟子非常惭愧，从此发奋读书，再也不逃学了。学习就像织布，要一梭一梭不间断，才能织出完整的布。"
  }
}
```

执行：按规范补全 `annotations.json` 至 **192 条全覆盖**（可分批写入文件），然后用完整性测试兜底（Task 4 会校验）。

- [ ] **Step 4: 生成 JSON 并抽查多音字**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python gen_content.py
# Expected: OK 6 chapters, 192 units -> ...
python - <<'EOF'
import json
d = json.load(open("../app/src/main/assets/content/sanzijing.json", encoding="utf-8"))
units = {u["id"]: u for c in d["chapters"] for u in c["units"]}
print(units["u005"]["phrases"][1]["pinyin"])   # zé lín chǔ
print(units["u189"]["phrases"][0]["pinyin"])   # rén wèi zǐ
print(sum(1 for u in units.values() if u.get("story")), "units with story")
EOF
```
Expected: `zé lín chǔ`、`rén wèi zǐ`、story 数量 ≥ 115（192 的 60%）。

- [ ] **Step 5: 逐条校对拼音输出（人工抽查）**

```bash
python - <<'EOF'
import json
d = json.load(open("../app/src/main/assets/content/sanzijing.json", encoding="utf-8"))
for c in d["chapters"]:
    for u in c["units"]:
        print(u["id"], u["phrases"][0]["hanzi"], u["phrases"][0]["pinyin"], "|", u["phrases"][1]["hanzi"], u["phrases"][1]["pinyin"])
EOF
```
通读输出，发现读音可疑的单元加入 `PINYIN_OVERRIDES` 后重跑 `python gen_content.py`。重点核对多音字：处/长/著/说/禅/泌/遗/传/并/干/别/且/为/当/中/数/著/载/应/与（"应乎中"的应 yìng 若标错同样加入覆盖表）。

- [ ] **Step 6: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add tools/gen_content.py tools/annotations.json app/src/main/assets/content/sanzijing.json
git commit -m "content: 内容生成管线 + 注释译文典故全量 192 条 → sanzijing.json"
```

---

### Task 4: Kotlin 内容模型 + ContentRepository + 完整性测试

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/data/content/Models.kt`, `app/src/main/java/com/rakor/sanzijing/data/content/ContentRepository.kt`
- Test: `app/src/test/java/com/rakor/sanzijing/TestFiles.kt`, `app/src/test/java/com/rakor/sanzijing/data/ContentIntegrityTest.kt`

- [ ] **Step 1: 写失败的内容完整性测试**

`app/src/test/java/com/rakor/sanzijing/TestFiles.kt`:
```kotlin
package com.rakor.sanzijing

import java.io.File

/** JVM 单测在 app/ 模块目录运行；此工具向上查找，兼容从项目根运行的情况。 */
object TestFiles {
    fun moduleFile(relPath: String): File {
        var dir: File? = File(System.getProperty("user.dir"))
        repeat(4) {
            val f = File(dir, relPath)
            if (f.exists()) return f
            dir = dir?.parentFile
        }
        error("$relPath not found from ${System.getProperty("user.dir")}")
    }
}
```

`app/src/test/java/com/rakor/sanzijing/data/ContentIntegrityTest.kt`:
```kotlin
package com.rakor.sanzijing.data

import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentIntegrityTest {
    private val repo = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }

    @Test
    fun `结构完整 - 6章 192单元`() {
        assertEquals(6, repo.chapters.size)
        assertEquals(192, repo.units.size)
    }

    @Test
    fun `单元id唯一且连续`() {
        val ids = repo.units.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals((1..192).map { "u%03d".format(it) }, ids)
    }

    @Test
    fun `每单元两个短语，各3字，拼音音节数与字数一致`() {
        repo.units.forEach { u ->
            assertEquals(2, u.phrases.size)
            u.phrases.forEach { p ->
                assertEquals(3, p.hanzi.length, "${u.id} hanzi=${p.hanzi}")
                assertEquals(p.hanzi.length, p.pinyin.split(" ").size, "${u.id} pinyin=${p.pinyin}")
            }
        }
    }

    @Test
    fun `注释译文齐全`() {
        repo.units.forEach { u ->
            assertTrue(u.annotation.isNotBlank(), "${u.id} annotation 空")
            assertTrue(u.translation.isNotBlank(), "${u.id} translation 空")
        }
    }

    @Test
    fun `典故字段成对出现`() {
        repo.units.forEach { u ->
            assertEquals(u.storyTitle != null, u.story != null, "${u.id} story 字段不成对")
            if (u.story != null) assertTrue(u.story!!.length >= 40, "${u.id} story 过短")
        }
    }

    @Test
    fun `典故覆盖率不低于百分之六十`() {
        val withStory = repo.units.count { it.story != null }
        assertTrue(withStory >= 115, "有典故的单元 $withStory < 115")
    }

    @Test
    fun `章节标题非空且id连续`() {
        repo.chapters.forEachIndexed { i, ch ->
            assertTrue(ch.title.isNotBlank())
            assertEquals("ch%02d".format(i + 1), ch.id)
            assertTrue(ch.units.isNotEmpty())
        }
    }
}
```

注意：`kotlin.test` 断言在纯 JVM 测试可用（kotlin-stdlib 自带）。JUnit4 `assertEquals` 参数顺序是 `(expected, actual, message)`。

- [ ] **Step 2: 运行测试确认失败**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.ContentIntegrityTest"
```
Expected: **编译失败**（`ContentRepository` 不存在）。

- [ ] **Step 3: 实现 Models 与 ContentRepository**

`app/src/main/java/com/rakor/sanzijing/data/content/Models.kt`:
```kotlin
package com.rakor.sanzijing.data.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentFile(val version: Int, val chapters: List<Chapter>)

@Serializable
data class Chapter(val id: String, val title: String, val units: List<LessonUnit>)

@Serializable
data class LessonUnit(
    val id: String,
    val phrases: List<Phrase>,
    val annotation: String,
    val translation: String,
    val storyTitle: String? = null,
    val story: String? = null,
) {
    val fullText: String get() = phrases.joinToString("，") { it.hanzi }
}

@Serializable
data class Phrase(val hanzi: String, val pinyin: String)
```

`app/src/main/java/com/rakor/sanzijing/data/content/ContentRepository.kt`:
```kotlin
package com.rakor.sanzijing.data.content

import kotlinx.serialization.json.Json
import java.io.InputStream

/** 内容只读仓库：启动后解析一次 assets JSON，进程内缓存。open 参数便于测试注入。 */
class ContentRepository(private val open: (String) -> InputStream) {

    private val json = Json { ignoreUnknownKeys = true }

    val content: ContentFile by lazy {
        open("content/sanzijing.json").use { stream ->
            json.decodeFromString<ContentFile>(stream.readBytes().decodeToString())
        }
    }

    val chapters: List<Chapter> get() = content.chapters
    val units: List<LessonUnit> get() = content.chapters.flatMap { it.units }

    fun unitIndexOf(unitId: String): Int = units.indexOfFirst { it.id == unitId }

    fun unitAt(index: Int): LessonUnit = units[index]

    fun chapterIndexOf(unitId: String): Int = chapters.indexOfFirst { ch -> ch.units.any { it.id == unitId } }

    fun chapterAt(index: Int): Chapter = chapters[index]

    fun firstUnitIdOfChapter(chapterIndex: Int): String = chapters[chapterIndex].units.first().id
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.ContentIntegrityTest"
```
Expected: `BUILD SUCCESSFUL`，7 个测试全过。若有失败，按失败信息修复 annotations.json 或脚本后重跑。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add app/src/main/java/com/rakor/sanzijing/data app/src/test
git commit -m "feat: 内容模型与仓库 + 内容完整性测试"
```

---

### Task 5: Room 进度层 + ProgressRepository + 单元测试

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/data/db/Entities.kt`, `Daos.kt`, `AppDatabase.kt`, `app/src/main/java/com/rakor/sanzijing/data/progress/ProgressRepository.kt`
- Modify: `SanzijingApp.kt`（AppContainer 加 db 与 progressRepository）
- Test: `app/src/test/java/com/rakor/sanzijing/data/ProgressRepositoryTest.kt`

- [ ] **Step 1: 写失败的 ProgressRepository 测试**

`app/src/test/java/com/rakor/sanzijing/data/ProgressRepositoryTest.kt`:
```kotlin
package com.rakor.sanzijing.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        var tick = 1_000_000L
        repo = ProgressRepository(
            progressDao = db.unitProgressDao(),
            statsDao = db.dailyStatsDao(),
            clock = { ++tick },
            today = { LocalDate.of(2026, 10, 3) },
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `播放完成标记已读并计数`() = runTest {
        repo.onPlaybackCompleted("u001")
        repo.onPlaybackCompleted("u001")
        val p = repo.observeProgress().first  // 用阻塞读取
        val e = db.unitProgressDao().get("u001")!!
        assertEquals(2, e.readCount)
        assertEquals(ProgressRepository.STATUS_READ, e.status)
        assertEquals(1, db.dailyStatsDao().get("2026-10-03")!!.unitsRead)
    }

    @Test
    fun `长按标记熟读再按取消`() = runTest {
        repo.toggleMastered("u002")
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u002")!!.status)
        repo.toggleMastered("u002")
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u002")!!.status)
    }

    @Test
    fun `继续学习返回第一个未学单元`() {
        val all = listOf("u001", "u002", "u003", "u004")
        assertEquals("u001", repo.continueUnitId(all, emptyList()))
        val p1 = com.rakor.sanzijing.data.db.UnitProgressEntity("u001", 1, 1, 100L, null)
        assertEquals("u002", repo.continueUnitId(all, listOf(p1)))
        val p2 = p1.copy(unitId = "u002", lastReadAt = 200L)
        assertEquals("u003", repo.continueUnitId(all, listOf(p1, p2)))
    }

    @Test
    fun `全部学完时继续学习回到最后互动单元`() {
        val all = listOf("u001", "u002")
        val p1 = com.rakor.sanzijing.data.db.UnitProgressEntity("u001", 1, 1, 100L, null)
        val p2 = com.rakor.sanzijing.data.db.UnitProgressEntity("u002", 2, 1, 300L, 300L)
        assertEquals("u002", repo.continueUnitId(all, listOf(p1, p2)))
    }

    @Test
    fun `学习时长按日累计`() = runTest {
        repo.addStudySeconds(30)
        repo.addStudySeconds(45)
        assertEquals(75L, db.dailyStatsDao().get("2026-10-03")!!.secondsStudied)
    }

    @Test
    fun `连续学习天数计算`() {
        fun day(s: Long) = DailyStatsEntity(date = "2026-10-%02d".format(s), secondsStudied = 60, unitsRead = 1)
        val stats = listOf(day(1), day(2), day(3))
        assertEquals(3, repo.studyStreak(stats, LocalDate.of(2026, 10, 3)))
        assertEquals(0, repo.studyStreak(stats, LocalDate.of(2026, 10, 5)))  // 断了
        assertEquals(3, repo.studyStreak(stats, LocalDate.of(2026, 10, 4)))  // 今天没学，但昨天连续
    }

    @Test
    fun `清除进度`() = runTest {
        repo.onPlaybackCompleted("u001")
        repo.clearAll()
        assertEquals(0, db.unitProgressDao().observeAllBlocking().size)
        assertEquals(0, db.dailyStatsDao().observeAllBlocking().size)
    }
}
```

阻塞读取辅助（加到 `Daos.kt`，仅测试用，直接返回 List 而非 Flow）:
```kotlin
@Query("SELECT * FROM unit_progress") suspend fun observeAllBlocking(): List<UnitProgressEntity>
@Query("SELECT * FROM daily_stats") suspend fun observeAllBlocking(): List<DailyStatsEntity>
```
（放在各自 DAO 接口中。测试里 `repo.observeProgress().first` 那行删除，改用 DAO 阻塞查询断言。）

- [ ] **Step 2: 运行测试确认失败**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.ProgressRepositoryTest"
```
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现 Room 层**

`app/src/main/java/com/rakor/sanzijing/data/db/Entities.kt`:
```kotlin
package com.rakor.sanzijing.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val status: Int,          // 0 未学 / 1 已读 / 2 已熟读
    val readCount: Int,
    val lastReadAt: Long,
    val markedAt: Long?,
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,   // yyyy-MM-dd
    val secondsStudied: Long,
    val unitsRead: Int,
)
```

`app/src/main/java/com/rakor/sanzijing/data/db/Daos.kt`:
```kotlin
package com.rakor.sanzijing.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitProgressDao {
    @Query("SELECT * FROM unit_progress")
    fun observeAll(): Flow<List<UnitProgressEntity>>

    @Query("SELECT * FROM unit_progress")
    suspend fun observeAllBlocking(): List<UnitProgressEntity>

    @Query("SELECT * FROM unit_progress WHERE unitId = :unitId")
    suspend fun get(unitId: String): UnitProgressEntity?

    @Query("SELECT * FROM unit_progress ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun lastRead(): UnitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UnitProgressEntity)

    @Query("DELETE FROM unit_progress")
    suspend fun clearAll()
}

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date")
    fun observeAll(): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats")
    suspend fun observeAllBlocking(): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun get(date: String): DailyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStatsEntity)

    @Query("DELETE FROM daily_stats")
    suspend fun clearAll()
}
```

`app/src/main/java/com/rakor/sanzijing/data/db/AppDatabase.kt`:
```kotlin
package com.rakor.sanzijing.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UnitProgressEntity::class, DailyStatsEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unitProgressDao(): UnitProgressDao
    abstract fun dailyStatsDao(): DailyStatsDao
}
```

`app/src/main/java/com/rakor/sanzijing/data/progress/ProgressRepository.kt`:
```kotlin
package com.rakor.sanzijing.data.progress

import com.rakor.sanzijing.data.db.DailyStatsDao
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressDao
import com.rakor.sanzijing.data.db.UnitProgressEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ProgressRepository(
    private val progressDao: UnitProgressDao,
    private val statsDao: DailyStatsDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    fun observeProgress(): Flow<List<UnitProgressEntity>> = progressDao.observeAll()
    fun observeDailyStats(): Flow<List<DailyStatsEntity>> = statsDao.observeAll()

    /** 播放某单元音频完成：readCount+1，status 至少为已读，当日 unitsRead+1。 */
    suspend fun onPlaybackCompleted(unitId: String) {
        val now = clock()
        val existing = progressDao.get(unitId)
        progressDao.upsert(
            UnitProgressEntity(
                unitId = unitId,
                status = maxOf(existing?.status ?: STATUS_NEW, STATUS_READ),
                readCount = (existing?.readCount ?: 0) + 1,
                lastReadAt = now,
                markedAt = existing?.markedAt,
            )
        )
        bumpToday(unitsDelta = 1)
    }

    /** 长按 OK：未熟读→熟读；已熟读→回退为已读。 */
    suspend fun toggleMastered(unitId: String) {
        val existing = progressDao.get(unitId)
            ?: UnitProgressEntity(unitId, STATUS_NEW, 0, clock(), null)
        val toMastered = existing.status != STATUS_MASTERED
        progressDao.upsert(
            existing.copy(
                status = if (toMastered) STATUS_MASTERED else STATUS_READ,
                markedAt = if (toMastered) clock() else null,
            )
        )
    }

    suspend fun addStudySeconds(seconds: Long) {
        val date = today().toString()
        val e = statsDao.get(date) ?: DailyStatsEntity(date, 0, 0)
        statsDao.upsert(e.copy(secondsStudied = e.secondsStudied + seconds))
    }

    suspend fun lastInteractedUnitId(): String? = progressDao.lastRead()?.unitId

    suspend fun clearAll() {
        progressDao.clearAll()
        statsDao.clearAll()
    }

    private suspend fun bumpToday(unitsDelta: Int) {
        val date = today().toString()
        val e = statsDao.get(date) ?: DailyStatsEntity(date, 0, 0)
        statsDao.upsert(e.copy(unitsRead = e.unitsRead + unitsDelta))
    }

    /** 继续学习 = 全文顺序第一个未学单元；全学完则回到最后互动单元。 */
    fun continueUnitId(allUnitIds: List<String>, progress: List<UnitProgressEntity>): String {
        val read = progress.filter { it.status > STATUS_NEW }.map { it.unitId }.toSet()
        return allUnitIds.firstOrNull { it !in read }
            ?: progress.maxByOrNull { it.lastReadAt }?.unitId
            ?: allUnitIds.first()
    }

    /** 连续学习天数：今天已学从今天数，否则从昨天往前数。 */
    fun studyStreak(stats: List<DailyStatsEntity>, today: LocalDate): Int {
        val days = stats.filter { it.secondsStudied > 0 || it.unitsRead > 0 }
            .map { LocalDate.parse(it.date) }.toSet()
        var d = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (d in days) { streak++; d = d.minusDays(1) }
        return streak
    }

    companion object {
        const val STATUS_NEW = 0
        const val STATUS_READ = 1
        const val STATUS_MASTERED = 2
    }
}
```

`SanzijingApp.kt` 的 AppContainer 修改为:
```kotlin
package com.rakor.sanzijing

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.progress.ProgressRepository

class AppContainer(appContext: Context) {
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "sanzijing.db").build()
    }
    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(db.unitProgressDao(), db.dailyStatsDao())
    }
}

class SanzijingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.ProgressRepositoryTest"
```
Expected: `BUILD SUCCESSFUL`，7 个测试全过（首次运行 Robolectric 会下载 android-all jar，走代理需一两分钟）。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: Room 进度层与 ProgressRepository（已读/熟读/时长/继续学习/连续天数）"
```

---

### Task 6: SettingsRepository（DataStore）

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/data/prefs/SettingsRepository.kt`
- Modify: `SanzijingApp.kt`（AppContainer 加 settingsRepository）
- Test: `app/src/test/java/com/rakor/sanzijing/data/SettingsRepositoryTest.kt`

- [ ] **Step 1: 写失败测试**

`app/src/test/java/com/rakor/sanzijing/data/SettingsRepositoryTest.kt`:
```kotlin
package com.rakor.sanzijing.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rakor.sanzijing.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {
    private fun newRepo(): SettingsRepository {
        val dir = File(System.getProperty("java.io.tmpdir"), "szj-${System.nanoTime()}").apply { mkdirs() }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { File(dir, "settings.preferences_pb") }
        return SettingsRepository(store)
    }

    @Test
    fun `默认值 自动朗读开 连读关 拼音开`() = runTest {
        val s = newRepo().settings.first()
        assertTrue(s.autoPlay)
        assertFalse(s.continuousPlay)
        assertTrue(s.showPinyin)
    }

    @Test
    fun `修改设置持久生效`() = runTest {
        val repo = newRepo()
        repo.setContinuousPlay(true)
        repo.setShowPinyin(false)
        val s = repo.settings.first()
        assertTrue(s.continuousPlay)
        assertFalse(s.showPinyin)
        assertTrue(s.autoPlay)
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.SettingsRepositoryTest"
```
Expected: 编译失败。

- [ ] **Step 3: 实现**

`app/src/main/java/com/rakor/sanzijing/data/prefs/SettingsRepository.kt`:
```kotlin
package com.rakor.sanzijing.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ReaderSettings(
    val autoPlay: Boolean = true,
    val continuousPlay: Boolean = false,
    val showPinyin: Boolean = true,
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val autoPlay = booleanPreferencesKey("auto_play")
    private val continuousPlay = booleanPreferencesKey("continuous_play")
    private val showPinyin = booleanPreferencesKey("show_pinyin")

    val settings: Flow<ReaderSettings> = store.data.map { p ->
        ReaderSettings(
            autoPlay = p[autoPlay] ?: true,
            continuousPlay = p[continuousPlay] ?: false,
            showPinyin = p[showPinyin] ?: true,
        )
    }

    suspend fun setAutoPlay(v: Boolean) { store.edit { it[autoPlay] = v } }
    suspend fun setContinuousPlay(v: Boolean) { store.edit { it[continuousPlay] = v } }
    suspend fun setShowPinyin(v: Boolean) { store.edit { it[showPinyin] = v } }
}
```

`SanzijingApp.kt` AppContainer 增加（import 相应类）:
```kotlin
    val settingsRepository: SettingsRepository by lazy {
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { appContext.preferencesDataStoreFile("settings") }
        SettingsRepository(store)
    }
```
需要 imports: `androidx.datastore.preferences.core.PreferenceDataStoreFactory`、`androidx.datastore.preferences.preferencesDataStoreFile`、`kotlinx.coroutines.CoroutineScope`、`kotlinx.coroutines.Dispatchers`、`kotlinx.coroutines.SupervisorJob`、`com.rakor.sanzijing.data.prefs.SettingsRepository`。AppContainer 构造参数 `appContext` 当前是 `Context` 类型，preferencesDataStoreFile 是 Context 的扩展函数，直接可用。

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.SettingsRepositoryTest"
```
Expected: 2 个测试全过。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: DataStore 设置仓库（自动朗读/连读/拼音显示）"
```

---

### Task 7: AudioPlayer（MediaPlayer 封装）

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/data/audio/AudioPlayer.kt`
- Modify: `SanzijingApp.kt`（AppContainer 加 audioPlayer）

无 JVM 单测（MediaPlayer 是框架类）；正确性由 Task 8 的 Fake 驱动 ViewModel 测试 + Task 15 真机验证。

- [ ] **Step 1: 实现**

`app/src/main/java/com/rakor/sanzijing/data/audio/AudioPlayer.kt`:
```kotlin
package com.rakor.sanzijing.data.audio

import android.content.res.AssetManager
import android.media.MediaPlayer

/** 音频播放抽象。assetPath 形如 "audio/u001.mp3"。 */
interface AudioPlayer {
    fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit)
    fun stop()
    fun release()
}

class MediaPlayerAudioPlayer(private val assets: AssetManager) : AudioPlayer {
    private var player: MediaPlayer? = null

    override fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        stop()
        try {
            val afd = assets.openFd(assetPath)   // 文件缺失/被压缩时抛 IOException
            val mp = MediaPlayer()
            player = mp
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { onComplete() }
            mp.setOnErrorListener { _, _, _ ->
                onError(java.io.IOException("playback failed: $assetPath"))
                true
            }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            player?.release()
            player = null
            onError(e)
        }
    }

    override fun stop() {
        player?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        player = null
    }

    override fun release() { stop() }
}
```

`SanzijingApp.kt` AppContainer 增加:
```kotlin
    val audioPlayer: AudioPlayer by lazy { MediaPlayerAudioPlayer(appContext.assets) }
```
（AppContainer 构造参数当前写的是 `appContext: Context`，保留该名字以匹配此行；import `com.rakor.sanzijing.data.audio.AudioPlayer` 与 `MediaPlayerAudioPlayer`。）

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: MediaPlayer 音频播放器（assets 逐句播放，缺失兜底）"
```

---

### Task 8: ReaderViewModel（核心学习逻辑）+ 单元测试

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/ui/reader/ReaderViewModel.kt`
- Test: `app/src/test/java/com/rakor/sanzijing/ui/ReaderViewModelTest.kt`

- [ ] **Step 1: 写失败测试**

`app/src/test/java/com/rakor/sanzijing/ui/ReaderViewModelTest.kt`:
```kotlin
package com.rakor.sanzijing.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.audio.AudioPlayer
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FakeAudioPlayer : AudioPlayer {
    val played = mutableListOf<String>()
    var onCompletion: (() -> Unit)? = null
    var failPaths: Set<String> = emptySet()
    var stopped = 0
    override fun play(assetPath: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        if (assetPath in failPaths) { onError(IOException("missing $assetPath")); return }
        played.add(assetPath)
        onCompletion = onComplete
    }
    override fun stop() { stopped++ }
    override fun release() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var content: ContentRepository
    private lateinit var player: FakeAudioPlayer
    private lateinit var scope: TestScope
    private lateinit var vm: ReaderViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        content = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }
        player = FakeAudioPlayer()
        var tick = 1_000_000L
        val progress = ProgressRepository(db.unitProgressDao(), db.dailyStatsDao(), clock = { ++tick })
        val dir = File(System.getProperty("java.io.tmpdir"), "szj-${System.nanoTime()}").apply { mkdirs() }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { File(dir, "settings.preferences_pb") }
        val settings = SettingsRepository(store)
        scope = TestScope(StandardTestDispatcher())
        vm = ReaderViewModel(
            startUnitId = "u001",
            contentRepo = content,
            progressRepo = progress,
            settingsRepo = settings,
            audioPlayer = player,
            externalScope = scope,
        )
        awaitReady()
    }

    @After
    fun tearDown() { db.close() }

    /** init 的加载在 IO 线程（Room/DataStore）+ 测试调度器上混合进行，轮询直到 ready。 */
    private fun awaitReady() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.ready) return
            Thread.sleep(20)
        }
        error("ViewModel not ready")
    }

    private fun settle() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.pendingWork == 0) return
            Thread.sleep(20)
        }
    }

    @Test
    fun `初始进入指定单元并自动朗读`() {
        assertEquals("u001", vm.state.value.unit.id)
        assertEquals(listOf("audio/u001.mp3"), player.played)
        assertEquals("教育之要", vm.state.value.chapterTitle)
        assertEquals(1, vm.state.value.chapterPos)
        assertEquals(14, vm.state.value.chapterSize)
        assertEquals(192, vm.state.value.totalCount)
    }

    @Test
    fun `前后翻句跨章连续，首尾不越界`() {
        vm.previous()
        assertEquals("u001", vm.state.value.unit.id)   // 首句 ◀ 不动
        repeat(15) { vm.next() }                        // 跨入第二章
        assertEquals("u016", vm.state.value.unit.id)
        assertEquals("蒙学伦理", vm.state.value.chapterTitle)
        repeat(300) { vm.next() }
        assertEquals("u192", vm.state.value.unit.id)   // 尾句 ▶ 不动
        vm.next()
        assertEquals("u192", vm.state.value.unit.id)
    }

    @Test
    fun `上下键跳章`() {
        vm.jumpChapter(1)
        assertEquals("u015", vm.state.value.unit.id)   // 第二章开头
        vm.jumpChapter(-1)
        assertEquals("u001", vm.state.value.unit.id)
        vm.jumpChapter(-1)
        assertEquals("u001", vm.state.value.unit.id)   // 已在第一章，不动
    }

    @Test
    fun `播放完成记已读，连读开启自动下一句`() {
        settle()
        vm.setContinuous(true)
        settle()
        player.onCompletion!!.invoke()
        settle()
        val p = db.unitProgressDao().get("u001")!!
        assertEquals(ProgressRepository.STATUS_READ, p.status)
        assertEquals(1, p.readCount)
        assertEquals("u002", vm.state.value.unit.id)
        assertEquals(listOf("audio/u001.mp3", "audio/u002.mp3"), player.played)
    }

    @Test
    fun `短按OK开关释义面板，长按OK标记熟读`() {
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertTrue(vm.state.value.showAnnotation)
        vm.okDown(); Thread.sleep(50); vm.okUp()
        assertFalse(vm.state.value.showAnnotation)

        vm.okDown(); Thread.sleep(700); vm.okUp()      // 长按
        assertFalse(vm.state.value.showAnnotation)
        settle()
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u001")!!.status)
        vm.okDown(); Thread.sleep(700); vm.okUp()
        settle()
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u001")!!.status)
    }

    @Test
    fun `菜单键开关操作面板`() {
        vm.toggleActionPanel()
        assertTrue(vm.state.value.showActionPanel)
        vm.toggleActionPanel()
        assertFalse(vm.state.value.showActionPanel)
    }

    @Test
    fun `重读当前句`() {
        player.played.clear()
        vm.replay()
        assertEquals("audio/u001.mp3", player.played.single())
    }

    @Test
    fun `音频缺失时提示且不崩溃`() {
        player.failPaths = setOf("audio/u001.mp3")
        vm.replay()
        assertTrue(vm.state.value.toast!!.contains("音频缺失"))
        assertFalse(vm.state.value.isPlaying)
        vm.consumeToast()
        assertEquals(null, vm.state.value.toast)
    }

    @Test
    fun `学习时长按秒累计`() {
        repeat(3) { vm.tickSecond() }
        settle()
        val all = db.dailyStatsDao().observeAllBlocking()
        assertTrue(all.sumOf { it.secondsStudied } >= 3)
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.ui.ReaderViewModelTest"
```
Expected: 编译失败（ReaderViewModel 不存在）。

- [ ] **Step 3: 实现 ReaderViewModel**

`app/src/main/java/com/rakor/sanzijing/ui/reader/ReaderViewModel.kt`:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.audio.AudioPlayer
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.content.LessonUnit
import com.rakor.sanzijing.data.prefs.ReaderSettings
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

data class ReaderUiState(
    val ready: Boolean = false,
    val pendingWork: Int = 0,
    val unit: LessonUnit,
    val unitIndex: Int = 0,
    val totalCount: Int = 0,
    val chapterTitle: String = "",
    val chapterPos: Int = 0,
    val chapterSize: Int = 0,
    val showAnnotation: Boolean = false,
    val showActionPanel: Boolean = false,
    val isPlaying: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
    val statusOf: Map<String, Int> = emptyMap(),
    val toast: String? = null,
) {
    val currentStatus: Int get() = statusOf[unit.id] ?: ProgressRepository.STATUS_NEW
}

class ReaderViewModel(
    startUnitId: String,
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    private val settingsRepo: SettingsRepository,
    private val audioPlayer: AudioPlayer,
    private val externalScope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val totalCount = contentRepo.units.size
    private var okDownAt: Long? = null

    private val _state = MutableStateFlow(stateFor(startIndex(startUnitId), ReaderUiState(unit = contentRepo.unitAt(0))))
    val state: StateFlow<ReaderUiState> = _state

    init {
        externalScope.launch {
            val settings = settingsRepo.settings.first()
            val progress = progressRepo.observeProgress().first()
            _state.update {
                stateFor(startIndex(startUnitId), it)
                    .copy(settings = settings, statusOf = progress.associate { e -> e.unitId to e.status }, ready = true)
            }
            if (settings.autoPlay) playCurrent()
        }
        externalScope.launch {
            settingsRepo.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
        externalScope.launch {
            progressRepo.observeProgress().collect { p ->
                _state.update { it.copy(statusOf = p.associate { e -> e.unitId to e.status }) }
            }
        }
    }

    private fun startIndex(unitId: String): Int = contentRepo.unitIndexOf(unitId).coerceIn(0, totalCount - 1)

    private fun stateFor(index: Int, prev: ReaderUiState): ReaderUiState {
        val unit = contentRepo.unitAt(index)
        val chapter = contentRepo.chapterAt(contentRepo.chapterIndexOf(unit.id))
        return prev.copy(
            unit = unit,
            unitIndex = index,
            totalCount = totalCount,
            chapterTitle = chapter.title,
            chapterPos = chapter.units.indexOfFirst { it.id == unit.id } + 1,
            chapterSize = chapter.units.size,
        )
    }

    fun next() = moveTo(_state.value.unitIndex + 1)
    fun previous() = moveTo(_state.value.unitIndex - 1)

    fun jumpChapter(delta: Int) {
        val target = (contentRepo.chapterIndexOf(_state.value.unit.id) + delta)
            .coerceIn(0, contentRepo.chapters.size - 1)
        val firstUnitId = contentRepo.firstUnitIdOfChapter(target)
        moveTo(contentRepo.unitIndexOf(firstUnitId))
    }

    fun replay() { playCurrent() }

    fun setContinuous(enabled: Boolean) {
        externalScope.launch { settingsRepo.setContinuousPlay(enabled) }
        _state.update { it.copy(settings = it.settings.copy(continuousPlay = enabled)) }
    }

    fun toggleActionPanel() = _state.update {
        it.copy(showActionPanel = !it.showActionPanel, showAnnotation = false)
    }

    /** OK 按下：记录时刻（忽略重复 down）。 */
    fun okDown() { if (okDownAt == null) okDownAt = now() }

    /** OK 抬起：>=600ms 长按标记熟读，短按切换释义面板。 */
    fun okUp() {
        val downAt = okDownAt ?: return
        okDownAt = null
        if (now() - downAt >= 600) {
            externalScope.launch { progressRepo.toggleMastered(_state.value.unit.id) }
        } else {
            _state.update { it.copy(showAnnotation = !it.showAnnotation, showActionPanel = false) }
        }
    }

    fun tickSecond() {
        externalScope.launch { progressRepo.addStudySeconds(1) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }

    private fun moveTo(index: Int, forcePlay: Boolean = false) {
        val clamped = index.coerceIn(0, totalCount - 1)
        if (clamped == _state.value.unitIndex && !forcePlay) return
        _state.update { stateFor(clamped, it) }
        if (forcePlay || _state.value.settings.autoPlay) playCurrent()
    }

    private fun playCurrent() {
        val unit = _state.value.unit
        audioPlayer.play(
            assetPath = "audio/${unit.id}.mp3",
            onComplete = { externalScope.launch { onPlaybackCompleted(unit.id) } },
            onError = { e ->
                _state.update { it.copy(isPlaying = false, toast = "音频缺失：${unit.fullText}") }
            },
        )
        _state.update { it.copy(isPlaying = true) }
    }

    private suspend fun onPlaybackCompleted(unitId: String) {
        progressRepo.onPlaybackCompleted(unitId)
        _state.update { it.copy(isPlaying = false) }
        val st = _state.value
        if (st.unit.id == unitId && st.settings.continuousPlay && st.unitIndex < totalCount - 1) {
            moveTo(st.unitIndex + 1, forcePlay = true)
        }
    }

    companion object {
        fun factory(startUnitId: String, app: SanzijingApp) = viewModelFactory {
            initializer {
                ReaderViewModel(
                    startUnitId = startUnitId,
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    settingsRepo = app.container.settingsRepository,
                    audioPlayer = app.container.audioPlayer,
                    externalScope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate),
                )
            }
        }
    }
}
```

`pendingWork` 字段测试用（统计在途协程）：在 `init` 的三个 `externalScope.launch` 与 `tickSecond`/`setContinuous`/`okUp` 长按分支里包裹计数。简化实现——在类里加:
```kotlin
    private fun tracked(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
        externalScope.launch {
            _state.update { it.copy(pendingWork = it.pendingWork + 1) }
            try { block() } finally { _state.update { s -> s.copy(pendingWork = s.pendingWork - 1) } }
        }
```
把 init 三个 launch 与所有 `externalScope.launch { ... }` 调用改为 `tracked { ... }`（onPlaybackCompleted 的 `externalScope.launch { onPlaybackCompleted(unit.id) }` 同样用 `tracked`）。

同时 `SanzijingApp.kt` 的 AppContainer 需补 `contentRepository`:
```kotlin
    val contentRepository: ContentRepository by lazy {
        ContentRepository { appContext.assets.open(it) }
    }
```
import `com.rakor.sanzijing.data.content.ContentRepository`。

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.ui.ReaderViewModelTest"
```
Expected: 9 个测试全过。若 `初始进入指定单元并自动朗读` 因 DataStore/Room 异步抖动失败，增大 `awaitReady` 轮询上限即可（不要删除轮询）。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: ReaderViewModel 读书核心逻辑（翻句/跳章/连读/熟读/时长）+ 测试"
```

---

### Task 9: 主题（童趣暖阳）+ 楷体字体子集

**Files:**
- Create: `app/src/main/java/com/rakor/sanzijing/ui/theme/Color.kt`, `Type.kt`, `Theme.kt`, `app/src/main/java/com/rakor/sanzijing/ui/common/PinyinPhrase.kt`
- Create: `tools/subset_font.py`, `app/src/main/res/font/wenkai_subset.ttf`（脚本生成）

- [ ] **Step 1: 下载 LXGW 文楷字体并安装 fonttools**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
export http_proxy=http://127.0.0.1:9999 https_proxy=http://127.0.0.1:9999
python -m pip install --quiet fonttools
curl -L -o LXGWWenKai-Regular.ttf https://github.com/lxgw/LxgwWenKai/releases/download/v1.510/LXGWWenKai-Regular.ttf
ls -la LXGWWenKai-Regular.ttf   # 应 >10MB；若 404，到 https://github.com/lxgw/LxgwWenKai/releases 查最新版本号替换 URL
```

- [ ] **Step 2: 写子集脚本并生成 res/font**

电视设备不带楷体系统字体，必须打包。子集后仅含全文+UI 用字（约 1–2MB）。

`tools/subset_font.py`:
```python
# -*- coding: utf-8 -*-
"""LXGW 文楷子集：只保留内容与 UI 用字，输出 res/font/wenkai_subset.ttf"""
import json, os
from fontTools import subset

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "LXGWWenKai-Regular.ttf")
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "font", "wenkai_subset.ttf")
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")

UI_TEXT = ("三字经继续学习章节教育之要蒙学伦理经典导读历史纲鉴勤学典故劝学结语"
           "学习报告设置已读熟连续天今日分钟总进度个句正在朗读跟着一起重开开关显示拼音"
           "清除确认取消版本内容基于通行版下一上更多操作跳转标记面板全部没有记录还没有"
           "开始学习吧退出释义典故注释译文小故事篇本章中第天启启")
EXTRA = "0123456789.%/：，。！？、（）·—…♪▶◀✓☰←→↑↓"

def collect_chars():
    chars = set(UI_TEXT + EXTRA)
    d = json.load(open(CONTENT, encoding="utf-8"))
    for c in d["chapters"]:
        chars.update(c["title"])
        for u in c["units"]:
            for p in u["phrases"]:
                chars.update(p["hanzi"])
                chars.update(p["pinyin"])   # 拼音声调字母 ā á ǎ à ǖ 等随内容一并收录
            chars.update(u["annotation"])
            chars.update(u["translation"])
            if u.get("storyTitle"):
                chars.update(u["storyTitle"])
            if u.get("story"):
                chars.update(u["story"])
    return "".join(sorted(chars))

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    options = subset.Options()
    options.notdef_outline = True
    font = subset.load_font(SRC, options)
    s = subset.Subsetter(options)
    s.populate(text=collect_chars())
    s.subset(font)
    subset.save_font(font, OUT, options)
    print("OK ->", os.path.abspath(OUT), os.path.getsize(OUT) // 1024, "KB")

if __name__ == "__main__":
    main()
```

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python subset_font.py
# Expected: OK -> ...wenkai_subset.ttf  (1000–2500 KB)
```

- [ ] **Step 3: 写主题代码**

`ui/theme/Color.kt`:
```kotlin
package com.rakor.sanzijing.ui.theme

import androidx.compose.ui.graphics.Color

val CreamTop = Color(0xFFFFF8EC)
val CreamMid = Color(0xFFFFEFD8)
val CreamBottom = Color(0xFFFFE6C7)
val Coral = Color(0xFFFF8A65)
val CoralDeep = Color(0xFFF07048)
val Honey = Color(0xFFD4954A)
val InkBrown = Color(0xFF5A4634)
val InkBrownFaded = Color(0xFF8A6D4E)
val CardWhite = Color(0xFFFFFFFF)
val CardFocused = Color(0xFFFFF2E2)
val ScrimBrown = Color(0x665A4634)
```

`ui/theme/Type.kt`:
```kotlin
package com.rakor.sanzijing.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.R

val WenKai = FontFamily(Font(R.font.wenkai_subset))

val AppTitleStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 8.sp, color = InkBrown)
val SectionLabelStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 4.sp, color = CoralDeep)
val HanziStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 64.sp, letterSpacing = 14.sp, color = InkBrown)
val PinyinStyle = TextStyle(fontFamily = WenKai, fontSize = 18.sp, letterSpacing = 5.sp, color = Honey)
val CardTitleStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBrown)
val BodyStyle = TextStyle(fontFamily = WenKai, fontSize = 17.sp, lineHeight = 30.sp, color = InkBrown)
val CaptionStyle = TextStyle(fontFamily = WenKai, fontSize = 14.sp, color = InkBrownFaded)
```

`ui/theme/Theme.kt`:
```kotlin
package com.rakor.sanzijing.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme

@Composable
fun SanzijingTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(CreamTop, CreamMid, CreamBottom)))
        ) { content() }
    }
}
```
文件顶部需 import `androidx.compose.runtime.Composable`。

`ui/common/PinyinPhrase.kt`:
```kotlin
package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.sanzijing.ui.theme.HanziStyle
import com.rakor.sanzijing.ui.theme.PinyinStyle

/** 汉字大字 + 上方拼音；读书页与面板共用。 */
@Composable
fun PinyinPhrase(
    hanzi: String,
    pinyin: String,
    showPinyin: Boolean,
    hanziSize: TextUnit = 64.sp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showPinyin) {
            androidx.compose.material3.Text(
                pinyin,
                style = PinyinStyle.copy(fontSize = hanziSize.value.dp.toSp() * 0.0f + hanziSize * 0.28f),
            )
        }
        androidx.compose.material3.Text(hanzi, style = HanziStyle.copy(fontSize = hanziSize))
    }
}
```
注意 `hanziSize * 0.28f`：TextUnit 乘法直接写 `hanziSize * 0.28f`（`androidx.compose.ui.unit` 支持 TextUnit.times(Float)）。拼音行样式一行改为:
```kotlin
style = PinyinStyle.copy(fontSize = hanziSize * 0.28f)
```
并把两处 `androidx.compose.material3.Text` 改为普通 `Text`（import `androidx.compose.material3.Text`）。修正后完整文件以实现为准，删除演示用的杂项 import。

- [ ] **Step 4: 接入 MainActivity 验证**

`MainActivity.kt` setContent 改为:
```kotlin
setContent {
    SanzijingTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PinyinPhrase("人之初", "rén zhī chū", showPinyin = true)
        }
    }
}
```
imports: `com.rakor.sanzijing.ui.theme.SanzijingTheme`、`com.rakor.sanzijing.ui.common.PinyinPhrase`。

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: 童趣暖阳主题 + 文楷字体子集 + 拼音汉字组件"
```

---

### Task 10: 导航 + 首页（继续学习/章节/报告/设置）

**Files:**
- Create: `ui/navigation/NavGraph.kt`, `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`, `ui/common/ProgressBarSimple.kt`
- Modify: `MainActivity.kt`
- Test: `app/src/test/java/com/rakor/sanzijing/ui/HomeViewModelTest.kt`

- [ ] **Step 1: 写失败的 HomeViewModel 测试**

`app/src/test/java/com/rakor/sanzijing/ui/HomeViewModelTest.kt`:
```kotlin
package com.rakor.sanzijing.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileInputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: HomeViewModel
    private lateinit var scope: TestScope
    private lateinit var progress: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val content = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }
        progress = ProgressRepository(db.unitProgressDao(), db.dailyStatsDao(), clock = { 1L })
        scope = TestScope(StandardTestDispatcher())
        vm = HomeViewModel(content, progress, scope, today = { LocalDate.of(2026, 10, 3) })
    }

    @After
    fun tearDown() { db.close() }

    private fun awaitReady() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.ready) return
            Thread.sleep(20)
        }
        error("not ready")
    }

    @Test
    fun `空进度时继续学习为 u001 且章节进度为零`() {
        awaitReady()
        val s = vm.state.value
        assertEquals("u001", s.continueUnitId)
        assertEquals("人之初，性本善", s.continueText)
        assertEquals("教育之要", s.continueChapterTitle)
        assertEquals(0, s.readCount)
        assertEquals(192, s.totalCount)
        assertEquals(6, s.chapters.size)
        assertEquals(14, s.chapters[0].total)
        assertEquals(60, s.chapters[3].total)
    }

    @Test
    fun `标记已读后继续学习前移并统计`() {
        awaitReady()
        scope.runCurrent()
        kotlinx.coroutines.test.advanceUntilIdle  // 无效引用占位删除此行
    }
}
```
第二个测试的正确写法（替换上面的占位测试体）:
```kotlin
    @Test
    fun `标记已读后继续学习前移并统计`() = kotlinx.coroutines.test.runTest { }
```
——不要用 runTest（会另起调度器）。直接写成:
```kotlin
    @Test
    fun `标记已读后继续学习前移并统计`() {
        awaitReady()
        // 直接写库模拟学习记录
        kotlinx.coroutines.runBlocking {
            progress.onPlaybackCompleted("u001")
            progress.onPlaybackCompleted("u002")
        }
        scope.runCurrent()
        Thread.sleep(50)
        scope.runCurrent()
        val s = vm.state.value
        assertEquals("u003", s.continueUnitId)
        assertEquals(2, s.readCount)
        assertEquals(2, s.chapters[0].read)
    }
```
（`kotlinx.coroutines.runBlocking` 需要 import 或写全限定名，如上。）

- [ ] **Step 2: 运行确认失败**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.ui.HomeViewModelTest"
```
Expected: 编译失败。

- [ ] **Step 3: 实现 HomeViewModel**

`ui/home/HomeViewModel.kt`:
```kotlin
package com.rakor.sanzijing.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressEntity
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChapterUi(
    val id: String,
    val title: String,
    val total: Int,
    val read: Int,
    val inProgress: Boolean,
)

data class HomeUiState(
    val ready: Boolean = false,
    val continueUnitId: String = "u001",
    val continueText: String = "",
    val continueChapterTitle: String = "",
    val readCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 0,
    val minutesToday: Long = 0,
    val chapters: List<ChapterUi> = emptyList(),
)

class HomeViewModel(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    externalScope: CoroutineScope,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(totalCount = contentRepo.units.size))
    val state: StateFlow<HomeUiState> = _state

    init {
        externalScope.launch {
            combine(progressRepo.observeProgress(), progressRepo.observeDailyStats(), ::compute)
                .collect { _state.value = it }
        }
    }

    private fun compute(
        progress: List<UnitProgressEntity>,
        stats: List<DailyStatsEntity>,
    ): HomeUiState {
        val statusOf = progress.associate { it.unitId to it.status }
        val contId = progressRepo.continueUnitId(contentRepo.units.map { it.id }, progress)
        val contUnit = contentRepo.units.first { it.id == contId }
        val contChapter = contentRepo.chapters.first { ch -> ch.units.any { it.id == contId } }
        val todayStr = today().toString()
        return HomeUiState(
            ready = true,
            continueUnitId = contId,
            continueText = contUnit.fullText,
            continueChapterTitle = contChapter.title,
            readCount = progress.count { it.status > ProgressRepository.STATUS_NEW },
            totalCount = contentRepo.units.size,
            streakDays = progressRepo.studyStreak(stats, today()),
            minutesToday = (stats.firstOrNull { it.date == todayStr }?.secondsStudied ?: 0) / 60,
            chapters = contentRepo.chapters.map { ch ->
                ChapterUi(
                    id = ch.id,
                    title = ch.title,
                    total = ch.units.size,
                    read = ch.units.count { (statusOf[it.id] ?: 0) > ProgressRepository.STATUS_NEW },
                    inProgress = ch.units.any { it.id == contId },
                )
            },
        )
    }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                HomeViewModel(
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.ui.HomeViewModelTest"
```
Expected: 2 个测试全过。

- [ ] **Step 5: 实现 ProgressBarSimple 与 HomeScreen**

`ui/common/ProgressBarSimple.kt`:
```kotlin
package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressBarSimple(fraction: Float, barColor: Color, trackColor: Color, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, RoundedCornerShape(height / 2))
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(height)
                .background(barColor, RoundedCornerShape(height / 2))
        )
    }
}
```

`ui/home/HomeScreen.kt`:
```kotlin
package com.rakor.sanzijing.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ProgressBarSimple
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralDeep
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.SectionLabelStyle
import com.rakor.sanzijing.ui.theme.TrackBrown

@Composable
fun HomeScreen(
    app: SanzijingApp,
    onContinue: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val continueFocus = remember { FocusRequester() }

    LaunchedEffect(state.ready) { if (state.ready) continueFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("三字经", style = AppTitleStyle)
            Spacer(Modifier.width(24.dp))
            Text(
                "已读 ${state.readCount} / ${state.totalCount} 句 · 连续学习 ${state.streakDays} 天 · 今日 ${state.minutesToday} 分钟",
                style = CaptionStyle,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(
                onClick = { onContinue(state.continueUnitId) },
                modifier = Modifier.weight(2.2f).height(150.dp).focusRequester(continueFocus),
                colors = cardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("▶ 继续学习", style = SectionLabelStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("「${state.continueText}」 · ${state.continueChapterTitle}", style = CardTitleStyle)
                    Spacer(Modifier.height(12.dp))
                    val fraction = if (state.totalCount == 0) 0f else state.readCount.toFloat() / state.totalCount
                    ProgressBarSimple(fraction, Coral, TrackBrown)
                    Spacer(Modifier.height(6.dp))
                    Text("总进度 ${(fraction * 100).toInt()}%", style = CaptionStyle)
                }
            }
            Card(
                onClick = onOpenReport,
                modifier = Modifier.weight(1f).height(150.dp),
                colors = cardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("📈 学习报告", style = CardTitleStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("进度 · 熟读 · 连续天数", style = CaptionStyle)
                }
            }
            Card(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).height(150.dp),
                colors = cardColors(),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("⚙ 设置", style = CardTitleStyle)
                    Spacer(Modifier.height(8.dp))
                    Text("朗读 · 连读 · 拼音", style = CaptionStyle)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("章节", style = SectionLabelStyle)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            state.chapters.forEach { ch ->
                Card(
                    onClick = { onContinue(firstUnitIdOfChapter(app, ch.id)) },
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = cardColors(),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(ch.title, style = com.rakor.sanzijing.ui.theme.CardTitleStyle.copy(fontSize = sp(18)))
                        Spacer(Modifier.height(6.dp))
                        Text("${ch.read} / ${ch.total} 句", style = CaptionStyle)
                        if (ch.inProgress) Text("学习中", style = SectionLabelStyle.copy(fontSize = sp(13)))
                    }
                }
            }
        }
    }
}

private fun firstUnitIdOfChapter(app: SanzijingApp, chapterId: String): String =
    app.container.contentRepository.chapters.first { it.id == chapterId }.units.first().id

@Composable
private fun cardColors() = CardDefaults.cardColors(
    containerColor = CardWhite,
    contentColor = com.rakor.sanzijing.ui.theme.InkBrown,
    focusedContainerColor = CardFocused,
    focusedContentColor = com.rakor.sanzijing.ui.theme.InkBrown,
)
```
注意修正两点（实现时直接写对）: `sp(18)` 应为 `18.sp`（import `androidx.compose.ui.unit.sp`）；`TrackBrown` 需在 Color.kt 增加 `val TrackBrown = Color(0x1A5A4634)`。`CardTitleStyle.copy(...)` 用全限定或顶部 import。

- [ ] **Step 6: 接入 NavGraph 与 MainActivity**

`ui/navigation/NavGraph.kt`:
```kotlin
package com.rakor.sanzijing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rakor.sanzijing.SanzijingApp

@Composable
fun AppNavGraph(app: SanzijingApp) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            com.rakor.sanzijing.ui.home.HomeScreen(
                app = app,
                onContinue = { unitId -> nav.navigate("reader/$unitId") },
                onOpenReport = { nav.navigate("report") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("reader/{unitId}") { entry ->
            val unitId = entry.arguments?.getString("unitId") ?: "u001"
            com.rakor.sanzijing.ui.reader.ReaderScreen(
                app = app,
                startUnitId = unitId,
                onBack = { nav.popBackStack() },
            )
        }
        composable("report") {
            com.rakor.sanzijing.ui.report.ReportScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            com.rakor.sanzijing.ui.settings.SettingsScreen(app = app, onBack = { nav.popBackStack() })
        }
    }
}
```
（Report/Settings 先建空实现文件使编译通过: `ReportScreen(onBack: () -> Unit)` 与 `SettingsScreen(app, onBack)` 各一个 `Text("TODO")`——Task 13 覆盖。空实现是临时的，Task 13 会替换为完整代码。）

`MainActivity.kt` setContent 改为:
```kotlin
setContent {
    SanzijingTheme {
        AppNavGraph(app = applicationContext as SanzijingApp)
    }
}
```

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 7: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: 导航图与首页（继续学习大卡/章节卡/报告/设置入口）"
```

---

### Task 11: 读书页（大字拼音、键位、自动朗读、时长）

**Files:**
- Create: `ui/reader/ReaderScreen.kt`, `ui/common/ToastHost.kt`
- Modify: `ui/reader/ReaderViewModel.kt`（补 closePanels/toggleMasteredNow）

- [ ] **Step 1: 实现 ToastHost**

`ui/common/ToastHost.kt`:
```kotlin
package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rakor.sanzijing.ui.theme.BodyStyle
import kotlinx.coroutines.delay

@Composable
fun ToastHost(toast: String?, onConsumed: () -> Unit) {
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            onConsumed()
        }
    }
    if (toast != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .background(Color(0xCC5A4634), RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(toast, style = BodyStyle.copy(color = Color.White))
            }
        }
    }
}
```
（`Text` import `androidx.compose.material3.Text`。）

- [ ] **Step 2: ReaderViewModel 补两个方法**

在 `ReaderViewModel` 类体内加:
```kotlin
    fun closePanels() = _state.update { it.copy(showAnnotation = false, showActionPanel = false) }

    fun toggleMasteredNow() {
        externalScope.launch { progressRepo.toggleMastered(_state.value.unit.id) }
    }
```

- [ ] **Step 3: 实现 ReaderScreen**

`ui/reader/ReaderScreen.kt`:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.PinyinPhrase
import com.rakor.sanzijing.ui.common.ToastHost
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.SectionLabelStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(app: SanzijingApp, startUnitId: String, onBack: () -> Unit) {
    val vm: ReaderViewModel = viewModel(factory = ReaderViewModel.factory(startUnitId, app))
    val state by vm.state.collectAsStateWithLifecycle()
    val content = app.container.contentRepository
    val anchorFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    val timerScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { anchorFocus.requestFocus() }
    LaunchedEffect(state.showActionPanel) {
        if (state.showActionPanel) panelFocus.requestFocus()
    }

    LifecycleResumeEffect(Unit) {
        val job = timerScope.launch {
            while (true) {
                delay(1_000)
                vm.tickSecond()
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    BackHandler(enabled = state.showAnnotation || state.showActionPanel) { vm.closePanels() }
    BackHandler(enabled = !state.showAnnotation && !state.showActionPanel) { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e -> handleKey(e, vm, state.showActionPanel) }
            .focusRequester(anchorFocus)
            .focusable()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.chapterTitle} · ${state.chapterPos} / ${state.chapterSize}",
                    style = SectionLabelStyle,
                )
                Spacer(Modifier.weight(1f))
                when (state.currentStatus) {
                    ProgressStatus.MASTERED -> StatusChip("✓ 已熟读")
                    ProgressStatus.READ -> StatusChip("已读")
                }
                Spacer(Modifier.width(16.dp))
                if (state.isPlaying) Text("♪ 朗读中 · 跟着一起读", style = CaptionStyle.copy(color = Coral))
            }
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinyinPhrase(
                    hanzi = state.unit.phrases[0].hanzi,
                    pinyin = state.unit.phrases[0].pinyin,
                    showPinyin = state.settings.showPinyin,
                )
                Box(
                    Modifier
                        .padding(horizontal = 40.dp)
                        .width(1.dp)
                        .height(90.dp)
                        .background(Color(0x33D4954A))
                )
                PinyinPhrase(
                    hanzi = state.unit.phrases[1].hanzi,
                    pinyin = state.unit.phrases[1].pinyin,
                    showPinyin = state.settings.showPinyin,
                )
            }
            Text(
                "◀ ▶ 前后句    ▲ ▼ 换章    OK 释义典故    播放键 重读    长按OK 熟读    菜单键 更多",
                style = CaptionStyle,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        if (state.showAnnotation) {
            AnnotationPanel(state = state, modifier = Modifier.align(Alignment.CenterEnd))
        }

        if (state.showActionPanel) {
            ActionPanel(
                state = state,
                chapters = content.chapters,
                vm = vm,
                panelFocus = panelFocus,
                onDismiss = { vm.closePanels() },
            )
        }

        ToastHost(toast = state.toast, onConsumed = { vm.consumeToast() })
    }
}

private object ProgressStatus {
    const val READ = 1
    const val MASTERED = 2
}

@Composable
private fun StatusChip(label: String) {
    Box(
        Modifier
            .background(Coral.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label, style = CaptionStyle.copy(color = Coral))
    }
}

/** 操作面板打开时，方向键/OK 交给面板内的焦点；菜单/媒体键仍由本页处理。 */
private fun handleKey(e: KeyEvent, vm: ReaderViewModel, actionPanelOpen: Boolean): Boolean {
    val key = e.key
    if (actionPanelOpen && key != Key.Menu && key != Key.Back) return false
    when (e.type) {
        KeyEventType.KeyUp -> return when (key) {
            Key.Menu -> { vm.toggleActionPanel(); true }
            Key.MediaPlayPause, Key.MediaPlay -> { vm.replay(); true }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { vm.okUp(); true }
            else -> false
        }
        KeyEventType.KeyDown -> return when (key) {
            Key.DirectionRight -> { vm.next(); true }
            Key.DirectionLeft -> { vm.previous(); true }
            Key.DirectionUp -> { vm.jumpChapter(-1); true }
            Key.DirectionDown -> { vm.jumpChapter(1); true }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { vm.okDown(); true }
            else -> false
        }
    }
    return false
}
```
注意: `when (e.type)` 的两个分支都 return，末行 `return false` 仅在类型为其它（如 Repeat 视作 KeyDown 之外的情形）时触达；`Key.Repeat` 类型为 `KeyEventType.KeyRepeat` 时不匹配任何分支落到 `return false`，方向键长按连续由系统重复 KeyDown 实现 ✓。`Key.Back` 不会被发到这里（返回键走系统 Back 处理），判断仅作防御。

- [ ] **Step 4: 编译验证（面板 Task 12 完成，此处先建空壳使编译通过）**

临时 `ui/reader/AnnotationPanel.kt`（Task 12 替换为完整版）:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AnnotationPanel(state: ReaderUiState, modifier: Modifier = Modifier) {
    Box(modifier)
}
```
临时 `ui/reader/ActionPanel.kt`（Task 12 替换）:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.rakor.sanzijing.data.content.Chapter

@Composable
fun ActionPanel(
    state: ReaderUiState,
    chapters: List<Chapter>,
    vm: ReaderViewModel,
    panelFocus: FocusRequester,
    onDismiss: () -> Unit,
) {
    Box(Modifier)
}
```

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: 读书页（大字拼音/键位映射/自动朗读/时长计时/底部键位提示）"
```

---

### Task 12: 释义典故面板 + 操作面板

**Files:**
- Rewrite: `ui/reader/AnnotationPanel.kt`, `ui/reader/ActionPanel.kt`（替换 Task 11 的空壳）
- Modify: `ui/reader/ReaderViewModel.kt`（UiState 增加 `chapterIndex`；增加 `jumpToChapter`）

- [ ] **Step 1: ReaderUiState 增加 chapterIndex 并补 VM 方法**

`ReaderUiState` 增加字段 `val chapterIndex: Int = 0`；`stateFor(...)` 的 copy 中增加 `chapterIndex = contentRepo.chapterIndexOf(unit.id)`。`jumpChapter(delta)` 改为委托:
```kotlin
    fun jumpChapter(delta: Int) = jumpToChapter(contentRepo.chapterIndexOf(_state.value.unit.id) + delta)

    fun jumpToChapter(index: Int) {
        val target = index.coerceIn(0, contentRepo.chapters.size - 1)
        moveTo(contentRepo.unitIndexOf(contentRepo.firstUnitIdOfChapter(target)))
    }
```
运行 `./gradlew testDebugUnitTest`，全部测试应保持通过。

- [ ] **Step 2: 完整 AnnotationPanel**

`ui/reader/AnnotationPanel.kt` 整体替换为:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.HanziStyle
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.SectionLabelStyle

/** 右侧滑出的释义典故面板（纯展示、无焦点，读书页按键不受影响）。 */
@Composable
fun AnnotationPanel(state: ReaderUiState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .width(460.dp)
            .padding(vertical = 24.dp, horizontal = 24.dp)
            .background(CardWhite, RoundedCornerShape(28.dp))
            .padding(32.dp)
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("释义 · ${state.unit.fullText}", style = SectionLabelStyle.copy(fontSize = 22.sp))
            Spacer(Modifier.height(18.dp))
            Text("注释", style = BodyStyle.copy(color = Coral, fontSize = 15.sp))
            Spacer(Modifier.height(6.dp))
            Text(state.unit.annotation, style = BodyStyle)
            Spacer(Modifier.height(18.dp))
            Text("译文", style = BodyStyle.copy(color = Coral, fontSize = 15.sp))
            Spacer(Modifier.height(6.dp))
            Text(state.unit.translation, style = BodyStyle)
            state.unit.storyTitle?.let { title ->
                Spacer(Modifier.height(22.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(androidx.compose.ui.graphics.Color(0x225A4634)))
                Spacer(Modifier.height(18.dp))
                Text("典故 · $title", style = BodyStyle.copy(color = Coral, fontSize = 16.sp))
                Spacer(Modifier.height(6.dp))
                Text(state.unit.story.orEmpty(), style = BodyStyle.copy(color = InkBrownFaded))
            }
            Spacer(Modifier.height(30.dp))
            Text("按 OK 或返回键关闭", style = BodyStyle.copy(color = InkBrownFaded, fontSize = 13.sp))
        }
    }
}
```
（`HanziStyle` 未用到可去掉该 import。）

- [ ] **Step 3: 完整 ActionPanel**

`ui/reader/ActionPanel.kt` 整体替换为:
```kotlin
package com.rakor.sanzijing.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.rakor.sanzijing.data.content.Chapter
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.InkBrownFaded
import com.rakor.sanzijing.ui.theme.ScrimBrown
import com.rakor.sanzijing.ui.theme.SectionLabelStyle

/** 菜单键呼出的操作面板：连读、熟读、跳章。打开时捕获焦点，方向键在面板内导航。 */
@Composable
fun ActionPanel(
    state: ReaderUiState,
    chapters: List<Chapter>,
    vm: ReaderViewModel,
    panelFocus: FocusRequester,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ScrimBrown)
            .focusRequester(panelFocus)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(600.dp)
                .background(CardWhite, RoundedCornerShape(28.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("操作", style = SectionLabelStyle.copy(fontSize = 24.sp))
            PanelRow("连读模式（播完自动下一句）", if (state.settings.continuousPlay) "开" else "关") {
                vm.setContinuous(!state.settings.continuousPlay)
            }
            PanelRow(
                "本句熟读 / 取消",
                when (state.currentStatus) {
                    ProgressStatus_MASTERED -> "已熟读"
                    ProgressStatus_READ -> "已读"
                    else -> "未学"
                },
            ) { vm.toggleMasteredNow() }
            Text("跳转章节", style = BodyStyle.copy(color = InkBrownFaded))
            chapters.chunked(3).forEach { rowChapters ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowChapters.forEach { ch ->
                        val index = chapters.indexOf(ch)
                        val isCurrent = index == state.chapterIndex
                        Card(
                            onClick = {
                                vm.jumpToChapter(index)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) CardFocused else CardWhite,
                                focusedContainerColor = CardFocused,
                            ),
                        ) {
                            Text(
                                ch.title + if (isCurrent) " ✦" else "",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                                style = BodyStyle.copy(fontSize = 15.sp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            PanelRow("关闭面板", "返回键 / OK") { onDismiss() }
        }
    }
}

private const val ProgressStatus_MASTERED = 2
private const val ProgressStatus_READ = 1

@Composable
private fun PanelRow(label: String, value: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = CardWhite,
            contentColor = InkBrown,
            focusedContainerColor = CardFocused,
            focusedContentColor = InkBrown,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = BodyStyle)
            Text(value, style = BodyStyle.copy(color = Coral))
        }
    }
}
```

- [ ] **Step 4: 编译 + 全量单测**

```bash
./gradlew testDebugUnitTest assembleDebug
```
Expected: `BUILD SUCCESSFUL`，全部既有测试通过。

- [ ] **Step 5: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: 释义典故面板与操作面板（连读/熟读/跳章）"
```

---

### Task 13: 学习报告页 + 设置页

**Files:**
- Rewrite: `ui/report/ReportScreen.kt`（替换占位），Create `ui/report/ReportViewModel.kt`
- Rewrite: `ui/settings/SettingsScreen.kt`（替换占位），Create `ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: ReportViewModel**

`ui/report/ReportViewModel.kt`:
```kotlin
package com.rakor.sanzijing.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressEntity
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChapterReport(val title: String, val read: Int, val total: Int, val mastered: Int)

data class ReportUiState(
    val ready: Boolean = false,
    val totalRead: Int = 0,
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val streakDays: Int = 0,
    val totalMinutes: Long = 0,
    val minutesToday: Long = 0,
    val chapters: List<ChapterReport> = emptyList(),
)

class ReportViewModel(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    externalScope: CoroutineScope,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState(totalCount = contentRepo.units.size))
    val state: StateFlow<ReportUiState> = _state

    init {
        externalScope.launch {
            combine(progressRepo.observeProgress(), progressRepo.observeDailyStats(), ::compute)
                .collect { _state.value = it }
        }
    }

    private fun compute(progress: List<UnitProgressEntity>, stats: List<DailyStatsEntity>): ReportUiState {
        val statusOf = progress.associate { it.unitId to it.status }
        val todayStr = today().toString()
        return ReportUiState(
            ready = true,
            totalRead = progress.count { it.status > ProgressRepository.STATUS_NEW },
            totalCount = contentRepo.units.size,
            masteredCount = progress.count { it.status == ProgressRepository.STATUS_MASTERED },
            streakDays = progressRepo.studyStreak(stats, today()),
            totalMinutes = stats.sumOf { it.secondsStudied } / 60,
            minutesToday = (stats.firstOrNull { it.date == todayStr }?.secondsStudied ?: 0) / 60,
            chapters = contentRepo.chapters.map { ch ->
                ChapterReport(
                    title = ch.title,
                    read = ch.units.count { (statusOf[it.id] ?: 0) > ProgressRepository.STATUS_NEW },
                    total = ch.units.size,
                    mastered = ch.units.count { statusOf[it.id] == ProgressRepository.STATUS_MASTERED },
                )
            },
        )
    }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                ReportViewModel(
                    contentRepo = app.container.contentRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
```

- [ ] **Step 2: ReportScreen 整体替换**

`ui/report/ReportScreen.kt`:
```kotlin
package com.rakor.sanzijing.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ProgressBarSimple
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardTitleStyle
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralDeep
import com.rakor.sanzijing.ui.theme.InkBrown
import com.rakor.sanzijing.ui.theme.SectionLabelStyle
import com.rakor.sanzijing.ui.theme.TrackBrown

@Composable
fun ReportScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val app = LocalContext.current.applicationContext as SanzijingApp
    val vm: ReportViewModel = viewModel(factory = ReportViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()

    Row(
        Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("学习报告", style = AppTitleStyle.copy(fontSize = 30.dp.value.sp))
            Spacer(Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center) {
                val fraction = if (state.totalCount == 0) 0f else state.totalRead.toFloat() / state.totalCount
                Canvas(Modifier.size(190.dp)) {
                    val stroke = Stroke(width = 22f, cap = StrokeCap.Round)
                    drawArc(Color(0x22FF8A65), -90f, 360f, useCenter = false, style = stroke)
                    drawArc(Coral, -90f, 360f * fraction, useCenter = false, style = stroke)
                }
                Text(
                    "${(fraction * 100).toInt()}%",
                    style = CardTitleStyle.copy(fontSize = 34.dp.value.sp, color = CoralDeep),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                "已读 ${state.totalRead} 句 · 熟读 ${state.masteredCount} 句\n连续学习 ${state.streakDays} 天 · 累计 ${state.totalMinutes} 分钟 · 今日 ${state.minutesToday} 分钟",
                style = CardTitleStyle.copy(fontSize = 20.dp.value.sp),
            )
            Spacer(Modifier.height(28.dp))
            if (state.totalRead == 0) {
                Text("还没有学习记录，回首页开始第一句吧！", style = CaptionStyle)
            }
            state.chapters.forEach { ch ->
                Text("${ch.title}（熟读 ${ch.mastered}）", style = CaptionStyle)
                Spacer(Modifier.height(6.dp))
                ProgressBarSimple(
                    fraction = if (ch.total == 0) 0f else ch.read.toFloat() / ch.total,
                    barColor = Coral,
                    trackColor = TrackBrown,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
```
注意修正: `30.dp.value.sp` 不是正确写法，应直接用 `30.sp`（import `androidx.compose.ui.unit.sp`），下同——实现时全部写 `XX.sp`。`InkBrown` 未用到可去 import。

- [ ] **Step 3: SettingsViewModel**

`ui/settings/SettingsViewModel.kt`:
```kotlin
package com.rakor.sanzijing.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.data.prefs.ReaderSettings
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ReaderSettings = ReaderSettings(),
    val showClearDialog: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val progressRepo: ProgressRepository,
    private val externalScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        externalScope.launch {
            settingsRepo.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
    }

    fun toggleAutoPlay() { externalScope.launch { settingsRepo.setAutoPlay(!_state.value.settings.autoPlay) } }
    fun toggleContinuous() { externalScope.launch { settingsRepo.setContinuousPlay(!_state.value.settings.continuousPlay) } }
    fun togglePinyin() { externalScope.launch { settingsRepo.setShowPinyin(!_state.value.settings.showPinyin) } }

    fun requestClearDialog(show: Boolean) = _state.update { it.copy(showClearDialog = show) }

    fun confirmClear() {
        externalScope.launch { progressRepo.clearAll() }
        _state.update { it.copy(showClearDialog = false, message = "学习进度已清除") }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    companion object {
        fun factory(app: SanzijingApp) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepo = app.container.settingsRepository,
                    progressRepo = app.container.progressRepository,
                    externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }
    }
}
```

- [ ] **Step 4: SettingsScreen 整体替换**

`ui/settings/SettingsScreen.kt`:
```kotlin
package com.rakor.sanzijing.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Dialog
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.common.ToastHost
import com.rakor.sanzijing.ui.theme.AppTitleStyle
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.CaptionStyle
import com.rakor.sanzijing.ui.theme.CardFocused
import com.rakor.sanzijing.ui.theme.CardWhite
import com.rakor.sanzijing.ui.theme.Coral
import com.rakor.sanzijing.ui.theme.CoralDeep
import com.rakor.sanzijing.ui.theme.InkBrown

@Composable
fun SettingsScreen(app: SanzijingApp, onBack: () -> Unit) {
    BackHandler { onBack() }
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Column(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 44.dp)) {
        Text("设置", style = AppTitleStyle.copy(fontSize = 30.sp))
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.width(720.dp)) {
            SettingRow("自动朗读当前句", if (state.settings.autoPlay) "开" else "关") { vm.toggleAutoPlay() }
            SettingRow("连读（播完自动下一句）", if (state.settings.continuousPlay) "开" else "关") { vm.toggleContinuous() }
            SettingRow("拼音显示", if (state.settings.showPinyin) "开" else "关") { vm.togglePinyin() }
            SettingRow("清除学习进度", "确认 ›") { vm.requestClearDialog(true) }
        }
        Spacer(Modifier.height(28.dp))
        Text("版本 1.0 · 内容基于通行版《三字经》", style = CaptionStyle)
    }

    if (state.showClearDialog) {
        ClearProgressDialog(
            onConfirm = { vm.confirmClear() },
            onDismiss = { vm.requestClearDialog(false) },
        )
    }
    ToastHost(toast = state.message, onConsumed = { vm.consumeMessage() })
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite,
            contentColor = InkBrown,
            focusedContainerColor = CardFocused,
            focusedContentColor = InkBrown,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = BodyStyle)
            Text(value, style = BodyStyle.copy(color = Coral))
        }
    }
}

@Composable
private fun ClearProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(520.dp)
                .background(CardWhite, RoundedCornerShape(28.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("清除学习进度？", style = BodyStyle.copy(color = CoralDeep, fontSize = 22.sp))
            Text("将删除全部已读/熟读记录与学习时长统计，且无法恢复。", style = BodyStyle)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    onClick = onConfirm,
                    colors = CardDefaults.cardColors(
                        containerColor = Coral,
                        contentColor = Color.White,
                        focusedContainerColor = CoralDeep,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text("确认清除", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = BodyStyle.copy(color = Color.White))
                }
                Card(
                    onClick = onDismiss,
                    colors = CardDefaults.cardColors(
                        containerColor = CardWhite,
                        focusedContainerColor = CardFocused,
                    ),
                ) {
                    Text("取消", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = BodyStyle)
                }
            }
        }
    }
}
```

- [ ] **Step 5: 编译 + 全量测试**

```bash
./gradlew testDebugUnitTest assembleDebug
```
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 6: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: 学习报告页（进度环/章节条/统计）与设置页（开关/清除进度对话框）"
```

---

### Task 14: 生成音频 + 图标，补音频完整性测试

**Files:**
- Create: `tools/gen_audio.py`, `tools/gen_icons.py`, `app/src/main/assets/audio/u001..u192.mp3`（脚本生成）
- Test: `app/src/test/java/com/rakor/sanzijing/data/AudioAssetsTest.kt`

- [ ] **Step 1: 写失败的音频完整性测试**

`app/src/test/java/com/rakor/sanzijing/data/AudioAssetsTest.kt`:
```kotlin
package com.rakor.sanzijing.data

import com.rakor.sanzijing.TestFiles
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioAssetsTest {
    @Test
    fun `每个单元都有对应且有效的音频文件`() {
        val dir = TestFiles.moduleFile("src/main/assets/audio")
        (1..192).forEach { i ->
            val f = File(dir, "u%03d.mp3".format(i))
            assertTrue(f.exists() && f.length() > 1_000, "${f.name} 缺失或过小（${f.length()}B）")
        }
    }
}
```

```bash
./gradlew testDebugUnitTest --tests "com.rakor.sanzijing.data.AudioAssetsTest"
```
Expected: FAIL（音频还不存在）。

- [ ] **Step 2: 写生成脚本并安装 edge-tts**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
export http_proxy=http://127.0.0.1:9999 https_proxy=http://127.0.0.1:9999
python -m pip install --quiet edge-tts
```

`tools/gen_audio.py`:
```python
# -*- coding: utf-8 -*-
"""edge-tts 批量生成逐句音频：assets/audio/uXXX.mp3（可整体替换真人音频）"""
import asyncio, json, os
import edge_tts

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "..", "app", "src", "main", "assets", "content", "sanzijing.json")
OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "audio")
VOICE = "zh-CN-XiaoxiaoNeural"   # 高质量中文女声；可换 zh-CN-XiaoyiNeural 更活泼
RATE = "-15%"                    # 放慢 15%，方便孩子跟读
CONCURRENCY = 4

async def gen(sem, unit_id, text):
    async with sem:
        path = os.path.join(OUT, f"{unit_id}.mp3")
        if os.path.exists(path) and os.path.getsize(path) > 1_000:
            return
        await edge_tts.Communicate(text, VOICE, rate=RATE).save(path)
        print("done", unit_id)

async def main():
    os.makedirs(OUT, exist_ok=True)
    data = json.load(open(CONTENT, encoding="utf-8"))
    sem = asyncio.Semaphore(CONCURRENCY)
    tasks = []
    for c in data["chapters"]:
        for u in c["units"]:
            text = u["phrases"][0]["hanzi"] + "，" + u["phrases"][1]["hanzi"]
            tasks.append(gen(sem, u["id"], text))
    await asyncio.gather(*tasks)
    files = [f for f in os.listdir(OUT) if f.endswith(".mp3")]
    print("total mp3:", len(files))
    assert len(files) >= 192, "音频数量不足"

asyncio.run(main())
```

- [ ] **Step 3: 生成 192 个音频（需联网，约几分钟）**

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python gen_audio.py
```
Expected: `total mp3: 192`。若个别失败（网络抖动），重跑脚本会跳过已存在的继续补齐。

- [ ] **Step 4: 测试转绿 + 打包验证**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest assembleDebug
```
Expected: 全部测试通过（含 `AudioAssetsTest`）。

- [ ] **Step 5: 生成正式图标与 banner**

`tools/gen_icons.py`:
```python
# -*- coding: utf-8 -*-
"""TV banner (640x360) 与启动图标：奶油渐变底 + 珊瑚印章 + 文楷字"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
FONT = os.path.join(HERE, "LXGWWenKai-Regular.ttf")

CREAM_TOP = (255, 248, 236)
CREAM_BOTTOM = (255, 230, 199)
CORAL = (255, 138, 101)
INK = (90, 70, 52)
SUB = (138, 109, 78)
WHITE = (255, 255, 255)

def gradient(size, top, bottom):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img

def make_banner():
    img = gradient((640, 360), CREAM_TOP, CREAM_BOTTOM)
    d = ImageDraw.Draw(img)
    d.text((250, 150), "三字经", font=ImageFont.truetype(FONT, 120), fill=INK, anchor="mm")
    d.text((250, 255), "亲子共读 · 逐句跟学", font=ImageFont.truetype(FONT, 36), fill=SUB, anchor="mm")
    d.rounded_rectangle(((470, 90), (590, 270)), radius=20, fill=CORAL)
    d.text((530, 180), "学", font=ImageFont.truetype(FONT, 96), fill=WHITE, anchor="mm")
    img.save(os.path.join(RES, "drawable-nodpi", "banner.png"))

def make_launcher():
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dpi, s in sizes.items():
        img = gradient((s, s), CREAM_TOP, CREAM_BOTTOM)
        d = ImageDraw.Draw(img)
        pad = max(2, s // 10)
        d.rounded_rectangle(((pad, pad), (s - pad, s - pad)), radius=s // 6, fill=CORAL)
        d.text((s / 2, s / 2 + s * 0.02), "三", font=ImageFont.truetype(FONT, s // 2), fill=WHITE, anchor="mm")
        os.makedirs(os.path.join(RES, f"mipmap-{dpi}"), exist_ok=True)
        img.save(os.path.join(RES, f"mipmap-{dpi}", "ic_launcher.png"))

make_banner()
make_launcher()
print("icons OK")
```

```bash
cd "D:/develop/src/rakor/threecharactorscripture/tools"
python gen_icons.py
# Expected: icons OK
```

- [ ] **Step 6: Commit**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "feat: edge-tts 生成 192 句占位音频 + 内容完整性音频测试 + 正式图标 banner"
```

---

### Task 15: 模拟器验收 + README + 收尾

**Files:**
- Create: `README.md`, `app/src/androidTest/java/com/rakor/sanzijing/AppSmokeTest.kt`

- [ ] **Step 1: 冒烟 instrumented 测试**

`app/src/androidTest/java/com/rakor/sanzijing/AppSmokeTest.kt`:
```kotlin
package com.rakor.sanzijing

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @Test
    fun mainActivityLaunchesWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { /* 渲染成功不崩溃即通过 */ }
        }
    }
}
```

- [ ] **Step 2: 创建 TV 模拟器（Android Studio 图形界面）**

1. Android Studio → Device Manager → Create Virtual Device → **TV** 类别 → 选 "Television (1080p)" → 系统镜像选 API 34 或 35（首次需下载，走系统代理或直接下载均可）
2. 启动该 AVD

命令行等价（可选）: `D:/develop/androidsdk/emulator/emulator -avd <名称>`。

- [ ] **Step 3: 安装并跑冒烟测试**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
export JAVA_HOME="/d/develop/soft/Android Studio/jbr" && export PATH="$JAVA_HOME/bin:$PATH"
./gradlew connectedDebugAndroidTest     # 需模拟器已启动
D:/develop/androidsdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: connected 测试通过，安装成功。

- [ ] **Step 4: 遥控器全流程走查（对照规格 §12 验收标准）**

在模拟器中逐项核对（模拟器键盘方向键 = D-pad，Enter = OK，Del = 返回）:

1. TV 桌面显示三字经图标与 banner，能启动
2. 首页默认焦点在「继续学习」；↕ 在两行卡片间移动，→ 移动到报告/设置
3. OK 进读书页：自动朗读 u001；▶ 连翻跨章；◀ 首句不越界；▲▼ 跳章
4. 短按 OK 出释义面板（注释/译文/典故），再按关闭；长按 OK（按住 1 秒）标记熟读，角标变化
5. 菜单键（模拟器键盘无菜单键时用 `adb shell input keyevent 82`）开操作面板：切连读、跳章、关闭
6. 连读开启后从某句开始自动逐句往下播；听 3 句后关闭连读
7. 返回首页 →「继续学习」指向下一句；学习报告页数字与操作一致（已读数、熟读数、进度环、章节条、连续天数）
8. 设置页三个开关逐一切换并回读书页验证生效（关拼音后读书页无拼音）
9. 清除进度 → 确认对话框 → 回首页全部归零
10. 读书页播放中按返回退回首页无崩溃；重新进入音频正常

任一项不符合：修复后重跑 `./gradlew testDebugUnitTest assembleDebug` 再走查。

- [ ] **Step 5: 写 README**

`README.md`:
```markdown
# 三字经 · Android TV 亲子共学

Compose for TV 构建的全离线《三字经》学习应用：逐句大字拼音、朗读跟读、释义典故、学习进度追踪。

## 开发环境

- Android Studio（本机: `D:\develop\soft\Android Studio`），SDK: `D:\develop\androidsdk`
- JDK 使用 Android Studio 自带 JBR（`gradle.properties` 已配置）
- 命令行构建需代理（已配置于 `gradle.properties`）

## 常用命令

```bash
./gradlew assembleDebug          # 打 debug 包
./gradlew testDebugUnitTest      # 全部单元测试
./gradlew connectedDebugAndroidTest  # 模拟器冒烟测试
```

## 替换真人音频

音频按单元编号存放于 `app/src/main/assets/audio/u001.mp3 … u192.mp3`。
用真人录制音频按**同名文件**覆盖该目录后重新打包即可，无需改任何代码。
重新生成占位音频: `cd tools && python gen_audio.py`。

## 修改内容（注释/译文/典故/章节）

1. 编辑 `tools/sanzijing_raw.txt`（文本）与 `tools/annotations.json`（注释/译文/典故）
2. `cd tools && python gen_content.py` 重新生成 `app/src/main/assets/content/sanzijing.json`
3. 若新增了生僻字/新拼音字符，重跑 `python subset_font.py` 更新字体子集

## 目录速览

- `docs/superpowers/specs/` 设计规格；`docs/superpowers/plans/` 实现计划
- `tools/` 内容与资产生成脚本（不参与打包）
```

- [ ] **Step 6: 最终提交**

```bash
cd "D:/develop/src/rakor/threecharactorscripture"
git add -A && git commit -m "docs: README 与验收走查；chore: 冒烟测试"
```

---

## 计划自审（Self-Review）

**1. 规格覆盖对照**

- 朗读+跟读（拼音大字/自动朗读/重读/连读）→ Task 8/11/14 ✓
- 释义+典故（面板/注释/译文/典故）→ Task 3/12 ✓
- 进度追踪（已读/熟读/继续学习/时长/连续天数/报告页）→ Task 5/10/13 ✓
- 设置（自动朗读/连读/拼音/清除进度确认）→ Task 6/13 ✓
- 键位映射（◀▶▲▼/OK短长按/播放键/菜单/返回）→ Task 8/11 ✓
- 视觉风格（童趣暖阳色值/楷体/焦点）→ Task 9 ✓（卡片焦点用 tv-material 默认缩放发光 + 奶油聚焦底色）
- 音频预置可替换/缺失兜底 → Task 7/14 + `noCompress mp3` ✓
- 错误处理（音频缺失提示/内容解析失败屏/DB 降级）→ Task 7（缺失 toast）✓；**内容解析失败全屏错误页与 DB 降级提示未单独实现**——内容 JSON 为打包资源且有完整性测试守护，DB 为 Room 默认行为；风险极低，接受此简化（与规格的"包损坏极罕见"判断一致）
- 验收标准 → Task 15 ✓

**2. 占位符扫描**: Task 3 的 annotations.json 除 6 条示例外按"撰写规范+素材清单+完整性测试兜底"执行——这是数据创作任务的明确契约，不是代码占位。Task 11/13 的临时空壳均在同计划后续任务中整体替换，已标注。

**3. 类型一致性抽查**: `LessonUnit`/`Chapter`/`ContentRepository` 各方法签名在 Task 4/8/10/11/12/13 间一致；`ProgressRepository.continueUnitId/studyStreak` 调用与定义一致；`AudioPlayer.play(assetPath, onComplete, onError)` 与 Fake 一致；路由 `"reader/{unitId}"` 与 ReaderScreen 参数一致；`ReaderUiState.chapterIndex`（Task 12 补充）被 ActionPanel 使用 ✓。

---

## 执行交接

计划已保存到 `docs/superpowers/plans/2026-10-03-sanzijing-tv-app.md`。两种执行方式：

**1. Subagent-Driven（推荐）**——每个任务派发独立子代理实现，任务间由主会话审查，迭代快
**2. Inline 执行**——本会话内用 executing-plans 按批次执行、设检查点审查




