# Interaction Queue Implementation Summary

## Overview
This implementation provides an offline-first interaction queue system that batches and deduplicates user interactions (likes, favorites, custom collections) before sending them to the backend.

## Key Features

### 1. **Deduplication**
- For each `contentId + collectionId` pair, only the latest interaction is kept
- Multiple rapid likes/unlikes on the same content result in a single final request
- Reduces server load and prevents race conditions

### 2. **Batch Processing**
- Waits 3 seconds (configurable via `BATCH_DELAY_MS`) to collect interactions
- Sends all queued interactions in a single batch request to `/collections/batch`
- Automatically retries failed requests up to 3 times

### 3. **Offline Support**
- Interactions are queued in Room database immediately
- Optimistic UI updates based on queued actions
- Automatic sync when network becomes available via `InteractionSyncService`

## Architecture

### Models

#### ActionType (Enum)
```kotlin
enum class ActionType {
    ADD,    // Add to collection / Like / Favorite
    REMOVE  // Remove from collection / Unlike / Unfavorite
}
```

#### QueuedInteractionEntity (Room Entity)
- Stores interactions in local database
- Fields: id, contentId, collectionId, action, timestamp, retryCount

#### CreateCollectionItemRequest (DTO)
- Sent to backend in batch requests
- Fields: contentId, collectionId (String: "likes", "favorites", or custom ID), action

### Components

#### InteractionQueueManager
**Location:** `data/manager/InteractionQueueManager.kt`

**Main Methods:**
- `queueLikeInteraction(contentId, isLike)` - Queue like/unlike
- `queueFavoriteInteraction(contentId, isFavorite)` - Queue favorite/unfavorite
- `queueCollectionInteraction(contentId, collectionId, isAdd)` - Queue custom collection add/remove
- `processQueuedInteractions()` - Process and send batch to server
- `getLatestAction(contentId, collectionId)` - Get optimistic state for UI

**Deduplication Logic:**
1. Fetch all queued interactions from database
2. Group by `contentId:collectionId` key
3. Keep only the latest interaction per key (highest timestamp)
4. Delete duplicates from database
5. Send remaining interactions in batch

#### InteractionRepository
**Location:** `data/repository/InteractionRepository.kt`

**Updated Methods:**
- `likeContent(contentId)` - Like content (adds to "likes" collection)
- `unlikeContent(contentId)` - Unlike content
- `favoriteContent(contentId)` - Favorite content (adds to "favorites" collection)
- `unfavoriteContent(contentId)` - Unfavorite content
- `addToCollection(contentId, collectionId)` - Add to custom collection
- `removeFromCollection(contentId, collectionId)` - Remove from custom collection
- `isContentLiked(contentId)` - Check like status (optimistic)
- `isContentFavorited(contentId)` - Check favorite status (optimistic)

#### InteractionSyncService
**Location:** `data/manager/InteractionSyncService.kt`

- Monitors network connectivity
- Automatically processes queue when network becomes available
- Already implemented, no changes needed

#### ApiClient
**Location:** `data/remote/ApiClient.kt` & `KtorApiClient.kt`

**New Method:**
- `batchProcessInteractions(interactions: List<CreateCollectionItemRequest>): Boolean`
  - Endpoint: `POST /collections/batch`
  - Sends batched interactions to server

### Database

#### New DAO: InteractionQueueDao
**Location:** `data/local/dao/InteractionQueueDao.kt`

**Methods:**
- `insert(interaction)` - Add interaction to queue
- `getAllQueued()` - Get all queued interactions
- `delete(id)` - Remove single interaction
- `deleteMultiple(ids)` - Remove multiple interactions
- `getQueueSize()` - Get queue count
- `getLatestForContent(contentId, collectionId)` - Get latest action for optimistic UI

#### Type Converters
**Location:** `domain/serialization/Converters.kt`

Converts `ActionType` enum to/from String for Room database storage.

## Backend Integration

### Collection-Based System
The backend now uses a unified collections system where:
- **Likes** = System collection with ID "likes"
- **Favorites** = System collection with ID "favorites"
- **Custom Collections** = User-created collections with String IDs

### Batch Endpoint
```kotlin
POST /collections/batch
Body: List<CreateCollectionItemRequest>

data class CreateCollectionItemRequest(
    val contentId: String,
    val collectionId: String,  // "likes", "favorites", or custom collection ID
    val action: ActionType     // ADD or REMOVE
)
```

The backend:
1. Processes all interactions in the batch
2. For each interaction, adds or removes the content from the specified collection
3. Returns success/failure status

## Usage Example

### In ViewModel or UseCase:
```kotlin
// Like a chart
interactionRepository.likeContent(contentId = 123uL)
    .collect { result ->
        result.onSuccess {
            // Interaction queued successfully
            // UI already updated optimistically
        }
    }

// Unlike a chart (multiple times rapidly)
interactionRepository.unlikeContent(contentId = 123uL) // Queued at T+0ms
delay(100)
interactionRepository.likeContent(contentId = 123uL)   // Queued at T+100ms
delay(100)
interactionRepository.unlikeContent(contentId = 123uL) // Queued at T+200ms

// After 3 seconds, only the LAST action (REMOVE) is sent to server
// The first two interactions are discarded as duplicates
```

### Check Optimistic State:
```kotlin
interactionRepository.isContentLiked(contentId = 123uL)
    .collect { result ->
        result.onSuccess { isLiked ->
            // Update UI based on latest queued action
            likeButton.isSelected = isLiked
        }
    }
```

## Configuration

### Adjustable Constants (in InteractionQueueManager):
- `BATCH_DELAY_MS = 3000L` - Time to wait before processing batch (milliseconds)
- `MAX_RETRIES = 3` - Maximum retry attempts for failed requests

## Benefits

1. **Reduced Server Load**: Multiple rapid interactions → single request
2. **Better UX**: Instant optimistic UI updates
3. **Offline Support**: Works without network, syncs when available
4. **Consistency**: Deduplication ensures final state matches user intent
5. **Reliability**: Automatic retries and persistent queue

## Migration Notes

### Database Version
The `AppDatabase` version should be incremented to include the new `QueuedInteractionEntity` table.

Current implementation references version 17 in `AppDatabase.kt`:
```kotlin
@Database(
    version = 17,
    entities = [
        Chart::class,
        Version::class,
        StreamingLink::class,
        QueuedInteractionEntity::class  // New entity
    ]
)
```

If your current version is different, update accordingly and provide migration strategy if needed.

## Testing Recommendations

1. **Rapid Interactions**: Click like/unlike repeatedly, verify only final state is sent
2. **Multiple Contents**: Like multiple charts quickly, verify batch contains all
3. **Offline Mode**: Turn off network, perform interactions, verify queue builds up
4. **Network Recovery**: Turn network back on, verify queue is processed automatically
5. **Retry Logic**: Simulate server errors, verify retries occur up to max attempts