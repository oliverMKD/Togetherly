package com.togetherly.content.schema

/**
 * Parser tests only — one pack, one quest, one instruction, free access, no timer. Not
 * production content.
 */
internal val VALID_MINIMAL_CATALOGUE_JSON = """
    {
      "schemaVersion": 1,
      "catalogueVersion": 1,
      "locale": "en",
      "packs": [
        {
          "id": "pack-1",
          "version": 1,
          "title": "Backyard Adventures",
          "description": "A pack of outdoor quests.",
          "access": { "type": "free" },
          "questIds": ["quest-1"],
          "artworkKey": "packs/backyard-adventures",
          "sortOrder": 0
        }
      ],
      "quests": [
        {
          "id": "quest-1",
          "version": 1,
          "title": "Backyard Scavenger Hunt",
          "summary": "Find five hidden treasures together.",
          "instructions": [
            { "order": 1, "text": "Hide five small objects in the yard." }
          ],
          "category": "discover",
          "ageBands": ["6-8"],
          "durationMinutes": 20,
          "location": "outdoor",
          "preparation": "simple-materials",
          "energy": "moderate",
          "completionPrompt": "Share what you found!",
          "packId": "pack-1",
          "access": { "type": "free" }
        }
      ]
    }
""".trimIndent()
