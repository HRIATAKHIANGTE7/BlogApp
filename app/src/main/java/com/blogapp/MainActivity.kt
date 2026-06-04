package com.hriata.blogapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

// I API KEY LEH BLOG ID - KA DAH LUT SA
const val API_KEY = "AIzaSyCL5wNCkFFkQy-FkDqIWzYN_TSasg4TFPQ" 
const val BLOG_ID = "126155628131705531"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlogAppTheme {
                BlogHomeScreen()
            }
        }
    }
}

// API Data Class
data class BloggerResponse(val items: List<BlogPost>?)
data class BlogPost(
    val id: String,
    val title: String,
    val published: String,
    val content: String,
    val labels: List<String>?,
    val images: List<PostImage>?
)
data class PostImage(val url: String)

// Retrofit API
interface BloggerApi {
    @GET("v3/blogs/{blogId}/posts")
    suspend fun getPosts(
        @Path("blogId") blogId: String,
        @Query("key") apiKey: String,
        @Query("fetchImages") fetchImages: Boolean = true,
        @Query("maxResults") maxResults: Int = 20
    ): BloggerResponse
}

// ViewModel
class BlogViewModel : ViewModel() {
    var posts by mutableStateOf<List<BlogPost>>(emptyList())
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    
    private val api = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/blogger/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BloggerApi::class.java)

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            try {
                val response = api.getPosts(BLOG_ID, API_KEY)
                posts = response.items ?: emptyList()
                error = null
            } catch (e: Exception) {
                error = "Blog load a fail: ${e.message}"
            }
            isLoading = false
        }
    }
}

// UI Theme - TechAsia Dark Mode
@Composable
fun BlogAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White,
            onBackground = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogHomeScreen(viewModel: BlogViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HRIATA BLOG", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = { BlogBottomNav() },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            viewModel.error != null -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(viewModel.error!!, color = Color.Red)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(viewModel.posts) { post ->
                        BlogCard(post)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun BlogCard(post: BlogPost) {
    val imageUrl = post.images?.firstOrNull()?.url ?: "https://picsum.photos/seed/${post.id}/400/200"
    val category = post.labels?.firstOrNull()?.uppercase() ?: "BLOG POST"
    val readTime = "${post.content.length / 1000 + 2} min read"
    val date = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
        formatter.format(parser.parse(post.published) ?: Date())
    } catch (e: Exception) {
        post.published.take(10)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = post.title,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = category,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = post.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Hriata • $date • $readTime",
                    fontSize = 12.sp,
                    color = Color.Gray
               