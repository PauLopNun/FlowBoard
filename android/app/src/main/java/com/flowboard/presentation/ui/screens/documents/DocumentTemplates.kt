package com.flowboard.presentation.ui.screens.documents

data class DocumentTemplate(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    /** Ordered list of (blockType, blockContent) pairs */
    val blocks: List<Pair<String, String>>
)

val builtInTemplates: List<DocumentTemplate> = listOf(
    DocumentTemplate(
        id = "meeting_notes",
        name = "Meeting Notes",
        description = "Structured notes with agenda and action items",
        emoji = "📋",
        blocks = listOf(
            "h1" to "Meeting Notes",
            "p" to "📅 Date:  ",
            "p" to "👥 Attendees:  ",
            "divider" to "",
            "h2" to "Agenda",
            "bullet" to "Item 1",
            "bullet" to "Item 2",
            "h2" to "Discussion Notes",
            "p" to "",
            "h2" to "Action Items",
            "todo" to "Action item 1",
            "todo" to "Action item 2",
            "h2" to "Next Steps",
            "p" to ""
        )
    ),
    DocumentTemplate(
        id = "project_plan",
        name = "Project Plan",
        description = "Goals, timeline and milestones for your project",
        emoji = "🚀",
        blocks = listOf(
            "h1" to "Project Plan",
            "callout" to "Brief overview of what this project aims to achieve",
            "divider" to "",
            "h2" to "Goals & Objectives",
            "bullet" to "Goal 1",
            "bullet" to "Goal 2",
            "h2" to "Timeline",
            "numbered" to "Phase 1: Discovery",
            "numbered" to "Phase 2: Development",
            "numbered" to "Phase 3: Testing",
            "numbered" to "Phase 4: Launch",
            "h2" to "Resources",
            "p" to "",
            "h2" to "Risks & Mitigation",
            "p" to ""
        )
    ),
    DocumentTemplate(
        id = "todo_list",
        name = "To-Do List",
        description = "Simple task checklist to stay organized",
        emoji = "✅",
        blocks = listOf(
            "h1" to "To-Do List",
            "divider" to "",
            "h2" to "Today",
            "todo" to "",
            "todo" to "",
            "todo" to "",
            "h2" to "This Week",
            "todo" to "",
            "todo" to "",
            "h2" to "Someday",
            "todo" to "",
            "todo" to ""
        )
    ),
    DocumentTemplate(
        id = "weekly_review",
        name = "Weekly Review",
        description = "Reflect on your week and plan the next one",
        emoji = "📅",
        blocks = listOf(
            "h1" to "Weekly Review",
            "divider" to "",
            "h2" to "Wins This Week",
            "bullet" to "",
            "bullet" to "",
            "h2" to "Challenges Faced",
            "p" to "",
            "h2" to "What I Learned",
            "p" to "",
            "h2" to "Next Week Goals",
            "todo" to "",
            "todo" to "",
            "todo" to ""
        )
    ),
    DocumentTemplate(
        id = "readme",
        name = "README",
        description = "Project documentation and setup guide",
        emoji = "📝",
        blocks = listOf(
            "h1" to "Project Name",
            "callout" to "Brief description of what this project does",
            "divider" to "",
            "h2" to "Getting Started",
            "h3" to "Prerequisites",
            "code" to "# Requirements",
            "h3" to "Installation",
            "code" to "# Installation steps",
            "h2" to "Usage",
            "p" to "Describe how to use the project",
            "h2" to "Contributing",
            "p" to "Contributions are welcome. Please open an issue first.",
            "h2" to "License",
            "p" to "MIT"
        )
    )
)
