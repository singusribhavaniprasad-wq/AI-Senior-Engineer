package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CodeReview
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.ReviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF090B0E)
                ) { innerPadding ->
                    ReviewWorkspaceScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Custom elite theme color constants
object EliteColors {
    val Background = Color(0xFF090B0E)
    val DeckBackground = Color(0xFF0F1218)
    val CodeStudioBg = Color(0xFF07090C)
    val ElectricCyan = Color(0xFF00F2FE)
    val ElectricTeal = Color(0xFF00D1FF)
    val CyberPurple = Color(0xFF9F2BFF)
    val ToxicGreen = Color(0xFF10B981)
    val WarningAmber = Color(0xFFFBBF24)
    val HighErrorRed = Color(0xFFEF4444)
    val SlateGutter = Color(0xFF1E293B)
    val SlateLight = Color(0xFF94A3B8)
    val BodyText = Color(0xFFCBD5E1)
}

@Composable
fun ReviewWorkspaceScreen(
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // UI state streams from ViewModel
    val historyList by viewModel.historyList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val currentReport by viewModel.currentReport.collectAsState()
    val errorMsg by viewModel.error.collectAsState()

    // Form inputs state
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedProjectType by remember { mutableStateOf("Android App") }
    var selectedReviewMode by remember { mutableStateOf("FAANG") }
    var codeSnippet by remember { mutableStateOf("") }

    // Bottom Navigation Bar state
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Workspace, 1 = Report, 2 = History

    val languages = listOf("Kotlin", "Java", "Python", "TypeScript", "C++", "Rust", "Go", "JavaScript", "SQL")
    val projectTypes = listOf("Android App", "Web Service/API", "Web App", "Library", "Smart Contract", "CLI Dev")
    val reviewModes = listOf(
        ReviewModeOption("FAANG", "Strict production rigor & algorithmic analysis", Icons.Default.Star, EliteColors.CyberPurple),
        ReviewModeOption("Startup CTO", "Velocity-optimized, shipping metrics, tech-debt tradeoffs", Icons.Default.Speed, EliteColors.ElectricTeal),
        ReviewModeOption("Security Hunter", "Deep vulnerability scanning and injection audits", Icons.Default.Security, EliteColors.HighErrorRed),
        ReviewModeOption("Bug Assassin", "Hunting null safety, concurrent loops, logic traps", Icons.Default.BugReport, EliteColors.WarningAmber),
        ReviewModeOption("Beginner Mentor", "Educational, step-by-step commentary & advice", Icons.Default.Info, EliteColors.ToxicGreen)
    )

    // Code template helper triggers
    val templates = listOf(
        CodeTemplate(
            name = "SQL Injection Vulnerability",
            lang = "SQL",
            proj = "Web Service/API",
            mode = "Security Hunter",
            code = """// INSECURE LOGIN LOGIC - DETECT THREAT VECTORS
public boolean authenticateUser(String userInput, String passwordInput) throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:sqlite:user_registry.db");
    Statement stmt = conn.createStatement();
    
    // ❌ Raw concatenation vulnerability triggering SQL Injection exploits
    String rawQuery = "SELECT * FROM administrators WHERE user = '" + userInput + "' AND pass = '" + passwordInput + "'";
    
    ResultSet rs = stmt.executeQuery(rawQuery);
    return rs.next();
}"""
        ),
        CodeTemplate(
            name = "Activity context reference Memory Leak",
            lang = "Kotlin",
            proj = "Android App",
            mode = "Bug Assassin",
            code = """package com.example.unstable

import android.app.Activity
import android.content.Context
import android.os.Bundle

class LeakActivity : Activity() {
    companion object {
        // ❌ Static reference pinning context object prevents garbage disposal (Memory Leak!)
        var globalPersistentContext: Context? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalPersistentContext = this
    }
}"""
        ),
        CodeTemplate(
            name = "Asynchronous Non-Atomic Race Condition",
            lang = "Kotlin",
            proj = "Android App",
            mode = "FAANG",
            code = """package com.example.concurrency

import kotlinx.coroutines.*

class CollisionRegistry {
    private var counterState = 0

    // ❌ Simultaneous non-atomic modifications over asynchronous scopes cause state race
    fun triggerRaceSimulation() {
        val backgroundScope = CoroutineScope(Dispatchers.Default)
        repeat(1000) {
            backgroundScope.launch {
                delay(1)
                counterState++
            }
        }
    }

    fun getCount(): Int = counterState
}"""
        )
    )

    // Side effect to sync tabs based on state transitions
    LaunchedEffect(currentReport) {
        if (currentReport != null) {
            currentTab = 1 // Switch to analyzer panel to show result immediately
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            currentTab = 1 // Switch to loading report state
        }
    }

    // Monitor API key
    val isApiKeyPresent = remember {
        val key = com.example.BuildConfig.GEMINI_API_KEY
        key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    // Main layout
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = EliteColors.DeckBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Coding workspace") },
                    label = { Text("Workspace", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EliteColors.ElectricCyan,
                        selectedTextColor = EliteColors.ElectricCyan,
                        unselectedIconColor = EliteColors.SlateLight,
                        unselectedTextColor = EliteColors.SlateLight,
                        indicatorColor = Color(0xFF131D2E)
                    ),
                    modifier = Modifier.testTag("nav_tab_workspace")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Active review board") },
                    label = { Text("Review Board", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EliteColors.ElectricCyan,
                        selectedTextColor = EliteColors.ElectricCyan,
                        unselectedIconColor = EliteColors.SlateLight,
                        unselectedTextColor = EliteColors.SlateLight,
                        indicatorColor = Color(0xFF131D2E)
                    ),
                    modifier = Modifier.testTag("nav_tab_report")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historical records") },
                    label = { Text("History logs", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EliteColors.ElectricCyan,
                        selectedTextColor = EliteColors.ElectricCyan,
                        unselectedIconColor = EliteColors.SlateLight,
                        unselectedTextColor = EliteColors.SlateLight,
                        indicatorColor = Color(0xFF131D2E)
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        },
        containerColor = EliteColors.Background
    ) { scaffoldPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            when (currentTab) {
                0 -> {
                    // TAB 0: WORKSPACE INPUT FORM
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            EliteAppHeader()
                        }

                        // API key missing warnings
                        if (!isApiKeyPresent) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A21)),
                                    border = BorderStroke(1.dp, EliteColors.HighErrorRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("api_key_error_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = EliteColors.HighErrorRed
                                        )
                                        Column {
                                            Text(
                                                text = "GEMINI_API_KEY Missing",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Please add your key in the Secrets panel in AI Studio side panel to enable autonomous reviews.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFCA5A5)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Demo template buttons row
                        item {
                            Text(
                                text = "TRY BUGGY TEMPLATES",
                                style = MaterialTheme.typography.labelSmall,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                templates.forEach { demo ->
                                    FilterChip(
                                        selected = codeSnippet == demo.code,
                                        onClick = {
                                            codeSnippet = demo.code
                                            selectedLanguage = demo.lang
                                            selectedProjectType = demo.proj
                                            selectedReviewMode = demo.mode
                                            Toast.makeText(context, "${demo.name} Loaded!", Toast.LENGTH_SHORT).show()
                                        },
                                        label = {
                                            Text(
                                                text = demo.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EliteColors.CyberPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = EliteColors.DeckBackground,
                                            labelColor = EliteColors.SlateLight
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = codeSnippet == demo.code,
                                            borderColor = EliteColors.SlateGutter,
                                            selectedBorderColor = EliteColors.CyberPurple
                                        ),
                                        modifier = Modifier.testTag("template_chip_${demo.name.replace(" ", "_")}")
                                    )
                                }
                            }
                        }

                        // Programming Language choice Row
                        item {
                            Text(
                                text = "PROGRAMMING LANGUAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                languages.forEach { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(text = lang, style = MaterialTheme.typography.bodySmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF132034),
                                            selectedLabelColor = EliteColors.ElectricCyan,
                                            containerColor = EliteColors.DeckBackground,
                                            labelColor = EliteColors.SlateLight
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedLanguage == lang,
                                            borderColor = EliteColors.SlateGutter,
                                            selectedBorderColor = EliteColors.ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("lang_chip_$lang")
                                    )
                                }
                            }
                        }

                        // Project Type choice Row
                        item {
                            Text(
                                text = "PROJECT ARCHITECTURE TYPE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                projectTypes.forEach { type ->
                                    FilterChip(
                                        selected = selectedProjectType == type,
                                        onClick = { selectedProjectType = type },
                                        label = { Text(text = type, style = MaterialTheme.typography.bodySmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF132034),
                                            selectedLabelColor = EliteColors.ElectricCyan,
                                            containerColor = EliteColors.DeckBackground,
                                            labelColor = EliteColors.SlateLight
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedProjectType == type,
                                            borderColor = EliteColors.SlateGutter,
                                            selectedBorderColor = EliteColors.ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("proj_chip_${type.replace("/", "_")}")
                                    )
                                }
                            }
                        }

                        // Review Mode Selector cards list
                        item {
                            Text(
                                text = "CODE REVIEW ALIGNMENT MODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reviewModes.forEach { mode ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedReviewMode = mode.name }
                                            .testTag("mode_card_${mode.name}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedReviewMode == mode.name) Color(0xFF121622) else EliteColors.DeckBackground
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (selectedReviewMode == mode.name) mode.themeColor else EliteColors.SlateGutter
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(mode.themeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = mode.icon,
                                                    contentDescription = mode.name,
                                                    tint = mode.themeColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = mode.desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = EliteColors.SlateLight,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Code input box (IDE style Terminal)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SOURCE CODE TO DECONSTRUCT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EliteColors.ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            clipboardManager.getText()?.let {
                                                codeSnippet = it.text
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste", fontSize = 11.sp, color = EliteColors.ElectricCyan)
                                    }
                                    TextButton(
                                        onClick = { codeSnippet = "" },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear", fontSize = 11.sp, color = EliteColors.HighErrorRed)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, EliteColors.SlateGutter, RoundedCornerShape(10.dp))
                            ) {
                                OutlinedTextField(
                                    value = codeSnippet,
                                    onValueChange = { codeSnippet = it },
                                    placeholder = {
                                        Text(
                                            text = "Paste or type a buggy or critical code block here to run architectural and bug-destruction review audits...",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF4B5563))
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .testTag("code_terminal_input"),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFF1F5F9)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = EliteColors.CodeStudioBg,
                                        unfocusedContainerColor = EliteColors.CodeStudioBg,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // Submit Execution Audit button
                        item {
                            Button(
                                onClick = {
                                    viewModel.executeCodeReview(
                                        selectedLanguage,
                                        selectedProjectType,
                                        selectedReviewMode,
                                        codeSnippet
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_review_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EliteColors.ElectricCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Analyze", tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DECONSTRUCT & AUDIT SNIPPET",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: REPORT COMPREHENSIVE SHEET & LOADER
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            isLoading -> {
                                LoadingAuditDashboard(loadingProgress)
                            }
                            errorMsg != null -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = EliteColors.HighErrorRed,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Deconstruction Aborted",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMsg ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EliteColors.SlateLight,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { currentTab = 0 },
                                        colors = ButtonDefaults.buttonColors(containerColor = EliteColors.ElectricCyan, contentColor = Color.Black)
                                    ) {
                                        Text("Return to Workspace")
                                    }
                                }
                            }
                            currentReport != null -> {
                                ActiveReportPanel(
                                    review = currentReport!!,
                                    viewModel = viewModel,
                                    onBackClicked = { viewModel.resetReport(); currentTab = 0 }
                                )
                            }
                            else -> {
                                // Default Empty State
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Empty",
                                        tint = EliteColors.SlateGutter,
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Active Report Analyzer",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No deconstruction review is currently generated. Complete the inputs in the Workspace and tap 'Deconstruct' to see AI deconstruction models.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EliteColors.SlateLight,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { currentTab = 0 },
                                        colors = ButtonDefaults.buttonColors(containerColor = EliteColors.ElectricCyan, contentColor = Color.Black)
                                    ) {
                                        Text("Open Workspace")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: HISTORY LOGS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "REDEEM HISTORY LOGS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${historyList.size} Saved Audit Cycles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EliteColors.SlateLight
                                )
                            }
                            if (historyList.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        viewModel.clearAllHistory()
                                        Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = EliteColors.HighErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear AllLogs", color = EliteColors.HighErrorRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (historyList.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Empty History",
                                    tint = EliteColors.SlateGutter,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Historic Cabins Empty",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Your analyzed and saved snippets will display here for offline review deconstruction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EliteColors.SlateLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(historyList) { item ->
                                    HistoryLogItemCard(
                                        review = item,
                                        onSelected = {
                                            viewModel.selectHistoryReview(item)
                                            currentTab = 1 // Open report
                                        },
                                        onDeleted = {
                                            viewModel.deleteReview(item)
                                            Toast.makeText(context, "Snippet removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EliteAppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(EliteColors.ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, EliteColors.ElectricCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Elite Logo",
                    tint = EliteColors.ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "AI SENIOR ENGINEER",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "ELITE ARCHITECT & BUG-DESTRUCTOR SUITE",
                    style = MaterialTheme.typography.labelSmall,
                    color = EliteColors.SlateLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = EliteColors.SlateGutter)
    }
}

@Composable
fun LoadingAuditDashboard(progressMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EliteColors.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High fidelity spinner deconstructor
        Box(
            modifier = Modifier
                .size(120.dp)
                .drawBehind {
                    drawCircle(
                        color = EliteColors.SlateGutter,
                        style = Stroke(width = 8.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                color = EliteColors.ElectricCyan,
                strokeWidth = 6.dp,
                trackColor = Color.Transparent
            )
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Thinking logic",
                tint = EliteColors.ElectricCyan,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "DECONSTRUCTING CODE NODE",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = progressMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = EliteColors.SlateLight,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.animateContentSize()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Running visual terminal activity logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = EliteColors.CodeStudioBg),
            border = BorderStroke(1.dp, EliteColors.SlateGutter)
        ) {
            Text(
                text = ">>> INITIALIZING HEAP AUDIT ON SNAPSHOT\n" +
                        ">>> SEARCHING BOUND VULNERABILITIES...\n" +
                        ">>> ALIGNING PATTERNS WITH AI KINETICS...\n" +
                        ">>> LAUNCHING REWRITE SIMULATIONS...",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = EliteColors.ToxicGreen.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ActiveReportPanel(
    review: CodeReview,
    viewModel: ReviewViewModel,
    onBackClicked: () -> Unit
) {
    val scores = remember(review) { viewModel.parseScoresFromText(review.reportContent) }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Executive & Critical, 1 = Raw Report
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EliteColors.Background)
    ) {
        // Applet top bar action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EliteColors.DeckBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = EliteColors.ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "DECONSTRUCT AUDIT",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = review.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = EliteColors.SlateLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(review.reportContent))
                    Toast.makeText(context, "Full Markdown Report Copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_markdown_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Copy full report",
                    tint = EliteColors.ElectricCyan
                )
            }
        }

        // Subtabs selection row
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = EliteColors.DeckBackground,
            contentColor = EliteColors.ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = EliteColors.ElectricCyan
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("EVALUATOR", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("report_subtab_eval")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("FULL REPORT", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("report_subtab_raw")
            )
        }

        // Display contents
        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() with
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { tabIndex ->
            when (tabIndex) {
                0 -> {
                    // TAB 0: SCORE METERS & HIGH ACTION ISSUES BOARD
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // OVERALL ENGINE READINESS ACCENT SCORE
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = EliteColors.DeckBackground),
                                border = BorderStroke(1.dp, EliteColors.SlateGutter)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DECONSTRUCTION QUOTIENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EliteColors.ElectricCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val qualityStatusText = when {
                                            review.score >= 8.5f -> "Enterprise Elite Grade"
                                            review.score >= 6.5f -> "Production Acceptable"
                                            review.score >= 4.5f -> "Action Needed Immediately"
                                            else -> "High-Risk Hazard Zone"
                                        }
                                        Text(
                                            text = qualityStatusText,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Aggregated score computed by evaluating 5 independent deconstruction metrics.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EliteColors.SlateLight
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { review.score / 10.0f },
                                            modifier = Modifier.fillMaxSize(),
                                            color = when {
                                                review.score >= 8.0f -> EliteColors.ToxicGreen
                                                review.score >= 6.0f -> EliteColors.WarningAmber
                                                else -> EliteColors.HighErrorRed
                                            },
                                            strokeWidth = 6.dp,
                                            trackColor = EliteColors.SlateGutter
                                        )
                                        Text(
                                            text = "%.1f".format(Locale.US, review.score),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // THE 5 COMPUTE SCORES ROW CARDS
                        item {
                            Text(
                                text = "CORE ARCHITECT METRICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricScoreBar(name = "Code Quality", score = scores.codeQuality, color = EliteColors.ElectricTeal)
                                MetricScoreBar(name = "Security Hardening", score = scores.security, color = EliteColors.HighErrorRed)
                                MetricScoreBar(name = "Performance Index", score = scores.performance, color = EliteColors.WarningAmber)
                                MetricScoreBar(name = "Maintainability Ratio", score = scores.maintainability, color = EliteColors.CyberPurple)
                                MetricScoreBar(name = "Production Readiness", score = scores.readiness, color = EliteColors.ToxicGreen)
                            }
                        }

                        // HIGH ALERT HIGHLIGHT
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF15191C)),
                                border = BorderStroke(1.dp, EliteColors.WarningAmber.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Tips",
                                        tint = EliteColors.WarningAmber
                                    )
                                    Text(
                                        text = "Tip: Access the 'FULL REPORT' tab up top to read a deeply detailed, line-by-line breakdown audit, simulated reviews, and multiple copyable code rewrites.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EliteColors.BodyText
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: FULL MARKDOWN REPORT VIEWER
                    Box(modifier = Modifier.fillMaxSize()) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            MarkdownViewer(review.reportContent)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricScoreBar(name: String, score: Int, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EliteColors.DeckBackground),
        border = BorderStroke(1.dp, EliteColors.SlateGutter)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$score / 10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { score / 10.0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = EliteColors.SlateGutter
            )
        }
    }
}

@Composable
fun HistoryLogItemCard(
    review: CodeReview,
    onSelected: () -> Unit,
    onDeleted: () -> Unit
) {
    val dateText = remember(review.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(review.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .testTag("history_item_${review.id}"),
        colors = CardDefaults.cardColors(containerColor = EliteColors.DeckBackground),
        border = BorderStroke(1.dp, EliteColors.SlateGutter)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = when {
                                review.score >= 8.0f -> EliteColors.ToxicGreen.copy(alpha = 0.15f)
                                review.score >= 6.0f -> EliteColors.WarningAmber.copy(alpha = 0.15f)
                                else -> EliteColors.HighErrorRed.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%.1f".format(Locale.US, review.score),
                        color = when {
                            review.score >= 8.0f -> EliteColors.ToxicGreen
                            review.score >= 6.0f -> EliteColors.WarningAmber
                            else -> EliteColors.HighErrorRed
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Column {
                    Text(
                        text = review.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateText • ${review.language}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EliteColors.SlateLight
                    )
                }
            }

            IconButton(
                onClick = onDeleted,
                modifier = Modifier.testTag("delete_history_item_button_${review.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete snippet run",
                    tint = EliteColors.SlateLight
                )
            }
        }
    }
}

// Custom simple Line Parser for Markdown representation in Compose
@Composable
fun MarkdownViewer(markdownText: String) {
    val lines = markdownText.split("\n")
    val codeLines = remember { mutableStateListOf<String>() }
    var inCodeBlock by remember { mutableStateOf(false) }
    var codeLanguage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // End previous block & Render it!
                    CodePreviewCard(code = codeLines.joinToString("\n"), language = codeLanguage)
                    codeLines.clear()
                    inCodeBlock = false
                    codeLanguage = ""
                } else {
                    // Start of code block
                    codeLanguage = trimmed.removePrefix("```").trim()
                    inCodeBlock = true
                }
            } else if (inCodeBlock) {
                codeLines.add(line)
            } else {
                when {
                    trimmed.startsWith("# ") -> {
                        Text(
                            text = trimmed.removePrefix("# "),
                            style = MaterialTheme.typography.headlineLarge,
                            color = EliteColors.ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
                        )
                    }
                    trimmed.startsWith("## ") -> {
                        Text(
                            text = trimmed.removePrefix("## "),
                            style = MaterialTheme.typography.titleLarge,
                            color = EliteColors.ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    }
                    trimmed.startsWith("### ") -> {
                        Text(
                            text = trimmed.removePrefix("### "),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFD2F7FF),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        Row(
                            modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 1.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EliteColors.ElectricCyan,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = line.removePrefix("- ").removePrefix("* ").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EliteColors.BodyText
                            )
                        }
                    }
                    trimmed.isEmpty() -> {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    else -> {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
        // Fallback for dangling open-ended code blocks
        if (inCodeBlock && codeLines.isNotEmpty()) {
            CodePreviewCard(code = codeLines.joinToString("\n"), language = codeLanguage)
        }
    }
}

@Composable
fun CodePreviewCard(code: String, language: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = EliteColors.CodeStudioBg),
        border = BorderStroke(1.dp, EliteColors.SlateGutter),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1017))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase().ifEmpty { "CODE EXCERPT" },
                    style = MaterialTheme.typography.labelSmall,
                    color = EliteColors.ElectricTeal,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Copy code block",
                        tint = EliteColors.SlateLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            HorizontalDivider(color = EliteColors.SlateGutter)
            Text(
                text = code,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF05070A))
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBDC9D8),
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

// Simple internal helper structures
data class ReviewModeOption(
    val name: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val themeColor: Color
)

data class CodeTemplate(
    val name: String,
    val lang: String,
    val proj: String,
    val mode: String,
    val code: String
)
