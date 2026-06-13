package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MediaViewModel
import com.example.ui.MediaViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database, client, and repo
        val db = AppDatabase.getDatabase(applicationContext)
        val emailSender = EmailSender()
        val repository = MediaRepository(db.mediaDao(), emailSender)
        
        // Set up the system layout
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Initialize modern viewmodel scoped with database factory
                val viewModel: MediaViewModel by viewModels { MediaViewModelFactory(repository) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MediaSenderScreen(viewModel = viewModel)
                }
            }
        }
SettingsSettings
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaSenderScreen(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    
    // Track photo, video camera captures
    var activeTempUri by remember { mutableStateOf<Uri?>(null) }
    var setupHelpOpened by remember { mutableStateOf(false) }
    
    // Check API Key
    val hasValidApiKey = remember {
        val apiKey = BuildConfig.SENDGRID_API_KEY
        !apiKey.isNullOrBlank() && 
                apiKey != "SG.gvhTB8mnRXq9WpFiv_WueA.XR6c2pEcEMZyD3814SettingsSettings
                
                SettingsSettings" && 
                apiKey != "MY_GEMINI_API_KEY" && 
                !apiKey.contains("SENDGRID_API_KEY")
    }

    // Launchers for media acquisition
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addPickedMedia(context, uri)
            Toast.makeText(context, "Adding media to queue...", Toast.LENGTH_SHORT).show()
        }
    }
    
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = activeTempUri
        if (success && uri != null) {
            viewModel.confirmCameraCapture(context, uri, isVideo = false)
            Toast.makeText(context, "Preserving photo and emailing...", Toast.LENGTH_SHORT).show()
        }
    }

    val takeVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = activeTempUri
        if (success && uri != null) {
            viewModel.confirmCameraCapture(context, uri, isVideo = true)
            Toast.makeText(context, "Preserving video and emailing...", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission checker triggers active launches safely
    var pendingActionIsVideo by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createTempFileUri(context, isVideo = pendingActionIsVideo)
            activeTempUri = uri
            if (pendingActionIsVideo) {
                takeVideoLauncher.launch(uri)
            } else {
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos/videos", Toast.LENGTH_LONG).show()
        }
    }

    fun initiateCameraCapture(isVideo: Boolean) {
        val cameraPermStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (cameraPermStatus == PackageManager.PERMISSION_GRANTED) {
            val uri = viewModel.createTempFileUri(context, isVideo = isVideo)
            activeTempUri = uri
            if (isVideo) {
                takeVideoLauncher.launch(uri)
            } else {
                takePictureLauncher.launch(uri)
            }
        } else {
            pendingActionIsVideo = isVideo
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            // Elegant Header Header with Status Banner
            HeaderView(
                hasValidApiKey = hasValidApiKey,
                onHelpRequest = { setupHelpOpened = true }
            )

            // Horizontal Button Grid for Media Ingestion
            MediaActionGrid(
                onTakePhoto = { initiateCameraCapture(isVideo = false) },
                onTakeVideo = { initiateCameraCapture(isVideo = true) },
                onSelectGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            )

            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            )

            // Dynamic Subtitle Queue Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Logs (${mediaItems.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                if (mediaItems.isNotEmpty()) {
                    Text(
                        text = "Auto-Transfers Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // List of queued transactions
            if (mediaItems.isEmpty()) {
                EmptyStateView(
                    onTakePhotoClick = { initiateCameraCapture(isVideo = false) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("media_logs_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mediaItems, key = { it.id }) { item ->
                        MediaItemRow(
                            item = item,
                            onRetry = { viewModel.retryItem(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet detailing setup instructions if key is missing
    if (setupHelpOpened) {
        AlertDialog(
            onDismissRequest = { setupHelpOpened = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Automated Setup Guide")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This application automatically emails captured photos and videos to rudrakshsinghlion@gmail.com.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To allow the app to dispatch background emails without user interaction, configure your SendGrid credentials:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "1. Open the Secrets Panel in your AI Studio visual sidebar.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "2. Add a new secret with Key: SENDGRID_API_KEY and set your verified SendGrid value.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "3. Add SENDGRID_SENDER_EMAIL with your verified sender domain/address.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        text = "Even if no keys are set, items are fully preserved on disk & in the database queue, ready to dispatch when you update parameters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { setupHelpOpened = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun HeaderView(
    hasValidApiKey: Boolean,
    onHelpRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Media Auto-Sender",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = "Continuous sync to mailbox",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Mail sync active",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Target Badge Information
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Sending to",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "To: rudrakshsinghlion@gmail.com",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Telemetry indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection State Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (hasValidApiKey) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                        .clickable { onHelpRequest() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasValidApiKey) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                    )
                    Text(
                        text = if (hasValidApiKey) "SMTP/SendGrid Linked" else "Key Required (Tap)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasValidApiKey) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    )
                }

                // Help Button
                TextButton(
                    onClick = onHelpRequest,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Setup GuideInfo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Setup Instructions",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun MediaActionGrid(
    onTakePhoto: () -> Unit,
    onTakeVideo: () -> Unit,
    onSelectGallery: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Take Photo
        Button(
            onClick = onTakePhoto,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("action_take_photo"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Take Photo", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }

        // Take Video
        Button(
            onClick = onTakeVideo,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("action_take_video"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Take Video", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }

        // Browse Gallery
        IconButton(
            onClick = onSelectGallery,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("action_select_gallery"),
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Select from gallery",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MediaItemRow(
    item: MediaItem,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Image Thumbnail Box
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.mimeType.startsWith("image")) {
                    AsyncImage(
                        model = File(item.filePath),
                        contentDescription = "Photo thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Video styling
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF263238))) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            // Metadatas and text logs
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.fileSize,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    
                    Text(
                        text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.timestamp)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Color Coded Queue Status indicator row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (item.status) {
                        MediaItem.STATUS_PENDING -> {
                            BadgeIndicator(
                                label = "Pending",
                                color = MaterialTheme.colorScheme.primary,
                                bgColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        MediaItem.STATUS_SENDING -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                BadgeIndicator(
                                    label = "Sending...",
                                    color = MaterialTheme.colorScheme.secondary,
                                    bgColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                        }
                        MediaItem.STATUS_SENT -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                BadgeIndicator(
                                    label = "Emailed",
                                    color = Color(0xFF2E7D32),
                                    bgColor = Color(0xFFE8F5E9)
                                )
                            }
                        }
                        MediaItem.STATUS_FAILED -> {
                            BadgeIndicator(
                                label = "Failed",
                                color = Color(0xFFC62828),
                                bgColor = Color(0xFFFFEBEE)
                            )
                        }
                    }
                }
                
                // Show descriptive failure details if present
                if (item.status == MediaItem.STATUS_FAILED && !item.errorReason.isNullOrBlank()) {
                    Text(
                        text = item.errorReason,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Quick actions side actions list
            val context = LocalContext.current
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share via local email client fallback
                IconButton(
                    onClick = {
                        try {
                            val authority = "${context.packageName}.fileprovider"
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                authority,
                                java.io.File(item.filePath)
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("rudrakshsinghlion@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "📸 Media Auto-Sender: ${item.fileName}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hello,\n\nHere is the media file '${item.fileName}' transferred to your mailbox automatically.\n\nMedia info:\n- Size: ${item.fileSize}\n- Type: ${item.mimeType}"
                                )
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Send via email client:"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error opening email client: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_share_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share via Email App",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (item.status == MediaItem.STATUS_FAILED) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("action_retry_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry sending email",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_delete_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeIndicator(label: String, color: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun ColumnScope.EmptyStateView(
    onTakePhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Media Captured",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Captured or selected photos/videos will automatically stream here and transfer directly to rudrakshsinghlion@gmail.com.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onTakePhotoClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Capture First Item")
        }
    }
}
