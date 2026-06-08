# 前端
- 畫面交互
- 按鈕功能
- 文本、圖片

## 畫面
| 來源畫面 (From) | 按鈕 | 目標畫面 (To) |
| :--- | :--- | :--- |
| `SplashScreen.kt` | 無 | `WelcomeScreen.kt` |
| `WelcomeScreen.kt` | (Begin your tour) | `LanguageSelectionScreen.kt` |
| `LanguageSelectionScreen.kt` | (Next) | `CultureInterestScreen.kt` |
| `CultureInterestScreen.kt` | (Start Exploring)、(Skip) | `HomeScreen.kt` |
| `HomeScreen.kt` | (more) | `RouteDetailScreen.kt` |
| (any) | (Home) | `HomeScreen.kt` |
| (any) | (Save) | `SavedScreen.kt` |
| (any) | (Memories) | `MemoriesScreen.kt` |
| `RouteDetailScreen.kt` | (任意景點) | `SpotIntroScreen.kt` |
| `SpotIntroScreen.kt` | (Info) | `SpotDetailScreen.kt` |
| `SpotIntroScreen.kt` | (任意建築) | `ArtifactDiscoveryScreen.kt` |
| `SpotIntroScreen.kt` | (Back) | (previous screen) |
| `SpotDetailScreen.kt` | (More) | `ArtifactDiscoveryScreen.kt` |
| `SpotDetailScreen.kt` | (Back) | (previous screen) |
| `ArtifactDiscoveryScreen.kt` | (More) | `ArtifactDetailScreen.kt` |
| `ArtifactDiscoveryScreen.kt` | (Camera) | `CameraPreviewScreen.kt` |
| `ArtifactDiscoveryScreen.kt` | (Back) | `SpotIntroScreen.kt` |
| `ArtifactDetailScreen.kt` | (Camera) | `CameraPreviewScreen.kt` |
| `SavedScreen.kt` | (more) | `RouteDetailScreen.kt` |
| `MemoriesScreen.kt` | (Create Memory) | `MemoryTemplateScreen.kt` |
| `MemoryTemplateScreen.kt` | (任意模板) | `MemoryPhotoSelectionScreen.kt` |
| `MemoryTemplateScreen.kt` | (Back) | (previous screen) |
| `MemoryPhotoSelectionScreen.kt` | (Next) | `MemoryEditScreen.kt` |
| `MemoryEditScreen.kt` | (Save) | `MemoryReflectionScreen.kt` |

## 其他按鈕
1. SplashScreen.kt
2. WelcomeScreen.kt
3. LanguageSelectionScreen.kt
    - 選擇英文、中文
4. CultureInterestScreen.kt
    - 選擇文化標籤
5. HomeScreen.kt
    - 搜尋欄
    - 篩選標籤
6. RouteDetailScreen.kt
    - (Saved) 收藏路線
7. SpotIntroScreen.kt
8. SpotDetailScreen.kt
    - (Podcast) 播Podcast {還沒有功能}
9. ArtifactDiscoveryScreen.kt
10. ArtifactDetailScreen.kt
11. CameraPreviewScreen.kt
12. SavedScreen.kt
13. MemoriesScreen.kt
    - (3 dots) 顯示操作選項：編輯、刪除、複製、分享
    - (arrow) 直接進入編輯 {應該要刪掉}
14. MemoryTemplateScreen.kt
15. MemoryPhotoSelectionScreen.kt
    - (Add Photos) 新增相片 {目前是直接發五張，沒呼叫相簿}
16. MemoryEditScreen.kt
    - (各種編輯功能) {還沒有功能}
17. MemoryReflectionScreen.kt
    - (My Reflection) 輸入文字
    - (Next) AI要生成文本 {目前是會直接跳掉}
    - (Regenerate) 重新生成 {第一次生成也可以搬到這裡?} {還沒有功能}
    - (Copy) 複製AI文本 {還沒有功能}
    - (Add to landmarks) 好像是新增地點資訊，我不確定 {還沒有功能}

<!-- ### 其他 -->

## 文本、圖片
1. SplashScreen.kt
2. WelcomeScreen.kt
    - welcome_subtitle
    - welcome_headline
    - welcome_description
    - begin_tour_button
3. LanguageSelectionScreen.kt
    - language_selection_subtitle
    - language_selection_headline
    - language_selection_description
    - language_english
    - language_chinese
    - next_button
4. CultureInterestScreen.kt
    - culture_interest_subtitle
    - culture_interest_headline
    - (各個標籤)
    - start_exploring_button
5. HomeScreen.kt
    - home_subtitle
    - home_headline
    - home_search_placeholder
    - (各個標籤)
    - (路線資料) {放在data.MockData的routes}
        - 圖片：sounds_of_temple.png、sea_protection.png、layers_of_colonial.png、brick_arches_and_time.png、faith_hidden.png
6. RouteDetailScreen.kt
    - (路線介紹)
        - 對應路線的圖片
    - (各個景點)
    - route_detail_your_journey
7. SpotIntroScreen.kt (景點目前只有Sound of Temple Tainan的Grand Mazu Temple，7.~10.)
    - discovery_mode
    - discovery_found
    - (返回鍵) 景點名稱
    - (地標名稱) {放在data.MockData的spots的discoveryItems}
    - 圖片：grand_mazu_temple.png(背景)、dragon_pillar.png、hanfan.png、mazu_statue.png
8. SpotDetailScreen.kt
    - (景點細節) {放在data.MockData的spots}
    - 圖片：eg1.png、eg2.png、eg3.png(黑白線稿圖片)
9. ArtifactDiscoveryScreen.kt
    - 地標介紹 {放在data.MockData的spots的discoveryItems}
    - 對應地標的圖片
10. ArtifactDetailScreen.kt
    - 地標詳細介紹 {放在data.MockData的spots的discoveryItems}
    - 對應地標的圖片
11. CameraPreviewScreen.kt
12. SavedScreen.kt
    - my_travel_plan
    - 路線
13. MemoriesScreen.kt
    - memories_headline
    - memories_in_progress
    - memories_completed
    - memories_continue_editing
    - memories_create_button
    - memories_menu_edit
    - memories_menu_delete
    - memories_menu_duplicate
    - memories_menu_share
    - memories_delete_confirm
    - 資料亂放的 {放在data.MockData的memories}
14. MemoryTemplateScreen.kt
    - memory_flow_choose_template
    - templates {放在data.MockData的templates}
    - 圖片：memory_templete_1.png、memory_templete_2.png、memory_templete_3.png、memory_templete_4.png、memory_templete_1_mask.png、memory_templete_2_mask.png、memory_templete_3_mask.png、memory_templete_4_mask.png
15. MemoryPhotoSelectionScreen.kt
    - memory_flow_import_photos
    - memory_flow_photos_selected
    - memory_flow_hold_to_reorder
    - memory_flow_add_photos
    - 圖片隨便塞的
16. MemoryEditScreen.kt
    - memory_flow_preview_edit
17. MemoryReflectionScreen.kt
    - memory_reflection_headline
    - memory_reflection_my_reflection
    - memory_reflection_polish_ai
    - memory_reflection_add_landmarks
    - memory_reflection_placeholder
