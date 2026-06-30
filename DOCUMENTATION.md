# Momentum: AI-Powered Proactive Productivity Companion

Momentum is a modern, high-fidelity Android productivity application developed to address the shortcomings of passive, easily-ignored reminder apps. By incorporating active real-time schedule analysis, custom task-deconstruction pathways, and dynamic interactive coaching, Momentum is engineered to keep users fully ahead of their deadlines.

---

## 1. Problem Statement Selected
**The Last-Minute Life Saver**

### Background
Students, professionals, and entrepreneurs frequently miss deadlines, assignments, meetings, bill payments, interviews, and important commitments. Existing productivity tools often rely on passive reminders that are easy to ignore and do little to help users actually complete their tasks.

### Challenge
Build an AI-powered productivity companion that proactively assists users in planning, prioritizing, and completing tasks before deadlines are missed. The solution should move beyond traditional reminders and focus on helping users take meaningful action.

---

## 2. Solution Overview
**Momentum** is a proactive, offline-first tactical assistant that moves past passive notification lists. The system is designed to act as an active, intelligent partner. Instead of merely listing tasks, Momentum analyzes upcoming deadlines, creates immediate step-by-step tactical deconstruction plans using advanced Generative AI, monitors active work schedules via customizable active feedback loops ("Focus Pulse"), and coaches users orally and textually using a voice-customized AI engine.

The visual design is structured on an immersive, high-contrast **Cosmic Slate Theme** optimized for focus. High-contrast negative spacing is paired with rich orange-gold status highlights (`#F97316` and `#FCD34D`). The primary interface features custom-designed vector layouts, including a modern, premium **tilted Hourglass (Sand Clock)** brand mark that symbolizes temporal flow and dynamic urgency.

---

## 3. Key Features

### 📅 Intelligent Deadlines & Automatic Task Deconstruction
*   **Deadline-Driven Deconstruction**: When a user creates a task, Momentum automatically analyzes the time remaining before the deadline.
*   **Time-Adaptive Action Plans**: Powered by Google’s Gemini API, Momentum deconstructs the task into **3 to 5 realistic, sequential steps**.
    *   *Long-Term Targets (>24 Hours)*: Broken down by logical daily phases (e.g., Day 1, Day 2, Day 3) to prevent procrastination.
    *   *Urgent Deadlines (<24 Hours)*: Broken down by precise minute-intervals (e.g., First 10m, Next 30m, Final 15m) to establish an immediate tactical runway.
*   **Flexible Progression**: Sub-steps are stored in a relational model and can be checked off, updated, or re-generated.

### ⚡ Focus Pulse: Customizable Active Reminder Engine
*   **Real-time Active Monitoring**: A persistent scheduling background thread that evaluates user focus windows and issues immediate notifications and logs to prevent distraction.
*   **Dynamic Time-of-Day Ranges**: Users configure specific times (e.g., 9:00 AM to 5:00 PM) and specific days of the week (e.g., Mon, Tue, Wed) when they want coaching to be active.
*   **Configurable Alert Frequency**: Supports rapid-interval alerts (customized down to several seconds for deep testing, up to several hours) to match user work pacing.

### 🎙️ Audio Coach (Text-to-Speech Voice Profiles)
*   **Interactive Vocal Feedback**: Integrates Android’s native Speech Synthesis (TTS) engine to speak reminders, status updates, and briefings aloud.
*   **Customized Personas**: Supports selecting different coach voices and vocal profiles, adding sensory reinforcement to daily productivity.

### 🧠 Strategic Proactive Calendar Analysis
*   **Optimize Calendar**: One-touch comprehensive schedule parsing. The AI scans the complete user task registry, identifies bottlenecks, computes priority weights, and yields a comprehensive, strategically written tactical briefing of the backlog.

### 💬 Tactical Coaching Chat
*   **Conversational AI Companion**: An integrated chat interface focused strictly on deadline mitigation, assignment planning, and distraction management.

---

## 4. Architecture & Technologies Used

Momentum is structured according to **Modern Android Development (MAD)** standards and the **MVVM (Model-View-ViewModel)** architectural pattern to guarantee robust performance, clean separation of concerns, and fluid UI rendering.

| Architectural Component | Technology / Library | Role & Functionality |
|---|---|---|
| **UI Framework** | **Jetpack Compose** | Fully declarative modern toolkit used to build smooth, responsive, custom-styled layouts. |
| **State Management** | **Kotlin Coroutines & Flow** | Manages high-performance asynchronous data streaming and thread coordination. |
| **Data Persistence** | **Android Room Database** | Local SQLite relational storage mapping tasks, sub-steps, settings, and histories offline-first. |
| **Dependency Injection**| **Constructor Injection** | Clean, lightweight dependency routing by coupling standard Kotlin parameters to ViewModels. |
| **Navigation** | **Navigation Compose** | High-performance, declarative, type-safe backstack route navigation. |
| **Design Language** | **Material Design 3 (M3)** | Modern UI structure, customized themes, adaptive dark palette integration, and fluid layouts. |

---

## 5. Google Technologies Utilized

### 🌌 Google Gemini API (via server-side or direct client orchestration)
Momentum uses Google’s high-performance **Gemini-2.5 / Gemini-3.5-Flash** models to orchestrate its core intelligence:
*   **Zero-Shot Structured Task Deconstruction**: Interprets task definitions and transforms vague objectives into structured, timeline-bound sub-actions without extra user input.
*   **Strategic Global Backlog Analysis**: Synthesizes tabular relational task registries to detect high-priority clusters, scheduling conflicts, and generate detailed schedule advisory reports.
*   **Conversational Context Parsing**: Drives the dynamic AI Coach, enabling situational, direct, and actionable advice.

### 🤖 Android Jetpack (Compose, Room, ViewModel, StateFlow)
*   **Jetpack Compose**: Powers the beautiful, custom Cosmic Slate typography, fluid transition states, dynamic progress trackers, and beautiful tilted sand-glass visualizers.
*   **Jetpack Room**: Supports complex relational operations (cascaded deletions, status updates, step tracking) to make the application 100% offline-ready.
*   **Jetpack Lifecycle & ViewModel**: Manages reliable, state-retaining view configurations across orientation changes, navigation cycles, and background processing events.
