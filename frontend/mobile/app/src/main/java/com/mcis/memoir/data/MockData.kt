package com.mcis.memoir.data

import com.mcis.memoir.R

object MockData {
    val routes = listOf(
        RouteData(
            id = "sounds_of_temple",
            titleEn = "Sounds of Temple Tainan",
            titleZh = "府城寺廟之聲",
            categoryEn = "Temples & Folk Beliefs",
            categoryZh = "寺廟與民俗",
            imageRes = R.drawable.sounds_of_temple,
            descriptionEn = "Faith that began at sea, moving inland with trade and governance.",
            descriptionZh = "信仰始於海洋，隨著貿易與治理向內陸延伸。",
            journeyItems = listOf(
                JourneyItem(1, "grand_mazu", "Grand Mazu Temple", "大天后宮"),
                JourneyItem(2, "grand_wumiao", "Grand Wumiao Temple", "祀典武廟"),
                JourneyItem(3, "heaven_temple", "Taiwan's First Heaven Temple", "台灣首廟天壇")
            )
        ),
        RouteData(
            id = "sea_protection",
            titleEn = "Sea Protection to City Beliefs",
            titleZh = "從海洋守護到城市信仰",
            categoryEn = "Temples & Folk Beliefs",
            categoryZh = "寺廟與民俗",
            imageRes = R.drawable.sea_protection,
            descriptionEn = "Faith that began at sea, moving inland with trade and governance.",
            descriptionZh = "信仰始於海洋，隨著貿易與治理向內陸延伸。",
            journeyItems = listOf(
                JourneyItem(1, "anping_kaitai", "Anping Kaitai Tianhou Temple", "安平開臺天后宮"),
                JourneyItem(2, "wind_god", "Temple of the Wind God", "風神廟"),
                JourneyItem(3, "grand_mazu", "Grand Mazu Temple", "大天后宮")
            )
        ),
        RouteData(
            id = "colonial_architecture",
            titleEn = "Layers of Colonial Architecture",
            titleZh = "日治建築的層次",
            categoryEn = "Historic Architecture",
            categoryZh = "古蹟建築",
            imageRes = R.drawable.layers_of_colonial,
            descriptionEn = "Explore the blending of Western styles and local needs during the Japanese period.",
            descriptionZh = "探索日治時期西方風格與在地需求的融合。",
            journeyItems = emptyList()
        ),
        RouteData(
            id = "brick_arches",
            titleEn = "Brick, Arches, and Time",
            titleZh = "磚牆、拱門與時光",
            categoryEn = "Historic Architecture",
            categoryZh = "古蹟建築",
            imageRes = R.drawable.brick_arches_and_time,
            descriptionEn = "A journey through the red brick structures that defined an era.",
            descriptionZh = "穿越定義了一個時代的紅磚建築之旅。",
            journeyItems = emptyList()
        ),
        RouteData(
            id = "faith_hidden",
            titleEn = "Faith Hidden in Alleyways",
            titleZh = "隱身巷弄的信仰",
            categoryEn = "Temples & Folk Beliefs",
            categoryZh = "寺廟與民俗",
            imageRes = R.drawable.faith_hidden,
            descriptionEn = "Small shrines with big stories tucked away in Tainan's old lanes.",
            descriptionZh = "藏在台南老巷弄裡、擁有大故事的小神龕。",
            journeyItems = emptyList()
        )
    )

    val spots = listOf(
        SpotData(
            id = "grand_mazu",
            titleEn = "Grand Mazu Temple",
            titleZh = "大天后宮",
            imageRes = R.drawable.grand_mazu_temple,
            durationEn = "30–45 mins",
            durationZh = "30–45 分鐘",
            whyItMattersEn = "The first officially built Mazu temple in Taiwan; bells and chanting create a distinctive soundscape, ideal for experiencing the auditory dimension of Tainan temple culture.",
            whyItMattersZh = "台灣第一座官建媽祖廟；鐘聲與誦經聲營造出獨特的聲景，是體驗台南寺廟文化聽覺維度的理想場所。",
            historicalContextEn = "In 1684, the Qing court converted the Zheng Kingdom's royal palace into this temple — signaling religious reconstruction after regime change.",
            historicalContextZh = "1684年，清廷將鄭氏政權的寧靖王府改建為這座廟宇，標誌著政權更迭後的宗教重建。",
            architecturalFeaturesEn = "Imperial-standard three-hall layout; main hall with five-bay width; double-eave hip-and-gable roof.",
            architecturalFeaturesZh = "皇室標準的三進式佈局；五開間寬的主殿；重簷歇山頂。",
            modernUseEn = "Now a national historic site and active faith center; annual Mazu birthday celebrations are large-scale events.",
            modernUseZh = "現為國定古蹟且為活躍的信仰中心；每年的媽祖誕辰慶典是大型盛事。",
            factsEn = listOf(
                "Main deity: Mazu",
                "Founded: 1684 (Qing Kangxi reign)",
                "Status: National historic site",
                "Note: First officially built Mazu temple in Taiwan; formerly Zheng Kingdom royal palace"
            ),
            factsZh = listOf(
                "主祀：媽祖",
                "創立：1684年（清康熙年間）",
                "地位：國定古蹟",
                "備註：台灣首座官建媽祖廟；原為寧靖王府"
            ),
            photographyTips = listOf(
                PhotographyTip(
                    id = 1,
                    descriptionEn = "Front panorama: Stand at the center of the courtyard, use wide angle to capture the symmetry of swallow-tail ridges — note the ceramic mosaic details on the roof.",
                    descriptionZh = "正面全景：站在中庭中央，使用廣角鏡頭捕捉燕尾脊的對稱美——注意屋頂上的剪黏細節。",
                    imageRes = R.drawable.sounds_of_temple
                ),
                PhotographyTip(
                    id = 2,
                    descriptionEn = "Incense burner close-up: Shoot from a low side angle; use rising smoke and the temple backdrop to create depth layers.",
                    descriptionZh = "香爐近景：從側面低角度拍攝；利用上升的煙霧和寺廟背景營造層次感。",
                    imageRes = R.drawable.sea_protection
                ),
                PhotographyTip(
                    id = 3,
                    descriptionEn = "Door-god paintings: Shoot close to the temple doors; focus on armor textures and the meticulous expression of the guardian deities.",
                    descriptionZh = "門神彩繪：靠近廟門拍攝；專注於盔甲的質感和守護神細膩的表情。",
                    imageRes = R.drawable.faith_hidden
                )
            ),
            discoveryItems = listOf(
                DiscoveryItem(
                    id = 1, 
                    labelEn = "Dragon Pillar", 
                    labelZh = "龍柱", 
                    imageRes = R.drawable.dragon_pillar, 
                    galleryImageRes = R.drawable.eg1,
                    questionEn = "How many dragons are on the pillars?", 
                    questionZh = "柱子上有多少條龍？",
                    moreInfoEn = "The coiling dragon pillar symbolizes protection and strength. In traditional belief, the dragon guards sacred spaces, carrying hopes for safety and harmony.",
                    moreInfoZh = "盤龍柱象徵著保護與力量。在傳統信仰中，龍守護著神聖的空間，承載著對平安與和諧的希望。"
                ),
                DiscoveryItem(
                    id = 2, 
                    labelEn = "Hanfan", 
                    labelZh = "憨番", 
                    imageRes = R.drawable.hanfan, 
                    galleryImageRes = R.drawable.eg2,
                    questionEn = "What is the Hanfan carrying?", 
                    questionZh = "憨番在搬什麼？",
                    moreInfoEn = "Hanfan are specialized architectural elements often found supporting the corners of roofs or beams, symbolizing service and strength.",
                    moreInfoZh = "憨番是常見於支撐屋頂角落或樑柱的特殊建築元素，象徵著服務與力量。"
                ),
                DiscoveryItem(
                    id = 3, 
                    labelEn = "Mazu Statue", 
                    labelZh = "媽祖像", 
                    imageRes = R.drawable.mazu_statue, 
                    galleryImageRes = R.drawable.eg3,
                    questionEn = "What color is the statue's face?", 
                    questionZh = "雕像的面部是什麼顏色？",
                    moreInfoEn = "The majestic Mazu statue is the heart of the temple. Her compassionate expression and detailed robes reflect the high craftsmanship of the era.",
                    moreInfoZh = "莊嚴的媽祖像是寺廟的核心。她慈悲的表情和細緻的長袍反映了那個時代的高超工藝。"
                )
            )
        ),
        SpotData(
            id = "grand_wumiao",
            titleEn = "Grand Wumiao Temple",
            titleZh = "祀典武廟",
            imageRes = R.drawable.faith_hidden,
            whyItMattersEn = "Dedicated to Guan Yu, the God of War; known for its solemn atmosphere and beautiful red walls.",
            whyItMattersZh = "主祀關聖帝君（戰神）；以其莊嚴的氛圍和美麗的朱紅山牆聞名。",
            discoveryItems = listOf(
                DiscoveryItem(1, "Red Wall", "朱紅山牆", R.drawable.faith_hidden),
                DiscoveryItem(2, "Plaque", "匾額", R.drawable.brick_arches_and_time)
            )
        ),
        SpotData(
            id = "anping_kaitai",
            titleEn = "Anping Kaitai Tianhou Temple",
            titleZh = "安平開臺天后宮",
            imageRes = R.drawable.sea_protection,
            whyItMattersEn = "Located at the historic Anping port, it was the landing point for many early settlers.",
            whyItMattersZh = "坐落於歷史悠久的安平港，是許多早期移民的登陸點。",
            discoveryItems = listOf(
                DiscoveryItem(1, "Port View", "安平港景", R.drawable.sea_protection),
                DiscoveryItem(2, "Stone Lion", "石獅", R.drawable.layers_of_colonial)
            )
        )
    )

    val memories = listOf(
        MemoryData(
            id = "mem_1",
            titleEn = "Tainan confucius temple",
            titleZh = "台南孔子廟",
            status = MemoryStatus.IN_PROGRESS,
            currentProgress = 2,
            totalProgress = 5,
            date = "2026/05/20",
            imageRes = R.drawable.sounds_of_temple
        ),
        MemoryData(
            id = "mem_2",
            titleEn = "Fort Oranje",
            titleZh = "熱蘭遮堡",
            status = MemoryStatus.COMPLETED,
            date = "2026/05/14",
            likes = 8,
            comments = 22,
            imageRes = R.drawable.sea_protection
        ),
        MemoryData(
            id = "mem_3",
            titleEn = "CHIMEI MUSEUM",
            titleZh = "奇美博物館",
            status = MemoryStatus.COMPLETED,
            date = "2026/02/20",
            likes = 16,
            comments = 152,
            imageRes = R.drawable.layers_of_colonial
        ),
        MemoryData(
            id = "mem_4",
            titleEn = "Tainan Art Museum",
            titleZh = "台南市美術館",
            status = MemoryStatus.COMPLETED,
            date = "2025/10/02",
            likes = 120,
            comments = 226,
            imageRes = R.drawable.faith_hidden
        )
    )

    val templates = listOf(
        TemplateData(
            id = "old_street",
            titleEn = "Old Street Journal",
            titleZh = "老街日誌",
            descriptionEn = "Using magazine-style layout and a journal-like feel, it records the old streets, arcades, shop signs, and daily life of the neighborhood.",
            descriptionZh = "使用雜誌風格的佈局和日記般的感覺，它記錄了街區的老街、拱廊、店鋪招牌和日常生活。",
            imageRes = R.drawable.memory_templete_1,
            maskRes = R.drawable.memory_templete_1_mask,
            slots = listOf(
                TemplateSlot(0.0138f, 0.1106f, 0.56f, 0.4426f, 0f), // Top left
                TemplateSlot(0.4687f, 0.0658f, 0.5313f, 0.2721f, 0f), // Top right
                TemplateSlot(0f, 0.4f, 0.3783f, 0.247f, 0f), // Middle left
                TemplateSlot(0.5717f, 0.2967f, 0.4283f, 0.5239f, 0f), // Middle right
                TemplateSlot(0f, 0.6274f, 0.543f, 0.3056f, 0f), // Bottom left
            )
        ),
        TemplateData(
            id = "city_walk",
            titleEn = "City Walk Map",
            titleZh = "城市漫步地圖",
            descriptionEn = "Record your day trip with maps, location markers, and photo cards.",
            descriptionZh = "使用地圖、地點標記和相片卡記錄您的日遊。",
            imageRes = R.drawable.memory_templete_2,
            maskRes = R.drawable.memory_templete_2_mask,
            slots = listOf(
                TemplateSlot(0.034f, 0f, 0.8459f, 0.4563f, 0f), // Top
                TemplateSlot(0.0266f, 0.3853f, 0.4463f, 0.2494f, 0f), // Middle left
                TemplateSlot(0.5292f, 0.4705f, 0.4357f, 0.1896f, 0f), // Middle right
                TemplateSlot(0.0223f, 0.6387f, 0.3858f, 0.1962f, 0f), // Bottom left
                TemplateSlot(0.5122f, 0.6666f, 0.4145f, 0.2147f, 0f), // Bottom right
            )
        ),
        TemplateData(
            id = "taiwan_pop",
            titleEn = "Taiwan Pop Collage",
            titleZh = "台灣流行拼貼",
            descriptionEn = "Create a youthful and eye-catching travel retrospective using stickers, handwritten text, contrasting labels, and photo collages.",
            descriptionZh = "使用貼紙、手寫文字、對比標籤和相片拼貼，打造充滿活力且吸睛的旅遊回顧。",
            imageRes = R.drawable.memory_templete_3,
            maskRes = R.drawable.memory_templete_3_mask,
            slots = listOf(
                TemplateSlot(0.0138f, 0.1106f, 0.56f, 0.4426f, 0f), // Top left
                TemplateSlot(0.4687f, 0.0658f, 0.5313f, 0.2721f, 0f), // Top right
                TemplateSlot(0f, 0.4f, 0.3783f, 0.247f, 0f), // Middle left
                TemplateSlot(0.5717f, 0.2967f, 0.4283f, 0.5239f, 0f), // Middle right
                TemplateSlot(0f, 0.6274f, 0.543f, 0.3056f, 0f), // Bottom left
            )
        ),
        TemplateData(
            id = "heritage_arch",
            titleEn = "Heritage Architecture Archive",
            titleZh = "文化遺產建築檔案",
            descriptionEn = "The exhibition catalogue-style layout preserves details of historical buildings, dates, locations, and cultural annotations.",
            descriptionZh = "展覽目錄風格的佈局保留了歷史建築、日期、地點和文化註釋的細節。",
            imageRes = R.drawable.memory_templete_4,
            maskRes = R.drawable.memory_templete_4_mask,
            slots = listOf(
                TemplateSlot(0.5078f, 0.1042f, 0.3438f, 0.2578f, 0f), // Top right
                TemplateSlot(0.1113f, 0.2702f, 0.4014f, 0.2454f, 0f), // Top left
                TemplateSlot(0.5078f, 0.3932f, 0.3135f, 0.2103f, 0f), // Middle right
                TemplateSlot(0.1611f, 0.5612f, 0.2676f, 0.1745f, 0f), // Bottom left
                TemplateSlot(0.4629f, 0.6185f, 0.4063f, 0.1816f, 0f), // Bottom right
            )
        )
    )
}
