package com.konasl.nagad

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// VirtualApp imports
import com.lody.virtual.client.core.VirtualCore
import com.lody.virtual.client.core.InstallResult
import com.lody.virtual.remote.InstallOptions

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appList: MutableList<AppInfo>
    private lateinit var clonedApps: MutableSet<String>
    private var isVirtualCoreReady = false

    companion object {
        private const val NAGAD_PACKAGE = "com.konasl.nagad"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Virtual Core first (Root-less virtualization)
        initVirtualCore()
        
        // Create UI Programmatically
        setupUI()
        
        loadClonedApps()
        loadInstalledApps()
    }
    
    private fun initVirtualCore() {
        try {
            VirtualCore.get().startup(this)
            VirtualCore.get().initialize()
            isVirtualCoreReady = true
            Toast.makeText(this, "Virtual Engine Ready", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            isVirtualCoreReady = false
            Toast.makeText(this, "Virtual Engine Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }

        // Header
        val titleText = TextView(this).apply {
            text = "Nagad Parallel Space"
            textSize = 24f
            setTextColor(Color.parseColor("#FF6B00")) // Nagad brand color
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        mainLayout.addView(titleText)
        
        // Subtitle
        val subtitleText = TextView(this).apply {
            text = "রুট ছাড়াই Nagad এর ডুয়াল অ্যাকাউন্ট"
            textSize = 14f
            setTextColor(Color.parseColor("#aaaaaa"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainLayout.addView(subtitleText)

        // Info card
        val infoCard = CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.parseColor("#2a2a2a"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        
        val infoText = TextView(this).apply {
            text = "📱 নির্বাচিত অ্যাপ ভার্চুয়াল স্পেসে চলে\n🔐 রুট ছাড়াই সম্পূর্ণ আলাদা ডাটা\n✨ Nagad সহ যেকোনো অ্যাপ ক্লোন করুন"
            textSize = 13f
            setTextColor(Color.parseColor("#cccccc"))
            setPadding(20, 15, 20, 15)
        }
        infoCard.addView(infoText)
        mainLayout.addView(infoCard)

        // RecyclerView
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(recyclerView)

        setContentView(mainLayout)
    }

    private fun loadClonedApps() {
        clonedApps = mutableSetOf()
        if (isVirtualCoreReady) {
            try {
                val installedApps = VirtualCore.get().installedApps
                clonedApps.addAll(installedApps.map { it.packageName })
            } catch (e: Exception) {
                // Fallback to local storage
                val prefs = getSharedPreferences("cloned_apps", MODE_PRIVATE)
                clonedApps.addAll(prefs.getStringSet("cloned_list", emptySet()) ?: emptySet())
            }
        }
    }

    private fun loadInstalledApps() {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        
        appList = mutableListOf()
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            
            // Skip self
            if (packageName == this.packageName) continue
            
            // Highlight Nagad in the list
            val isNagad = (packageName == NAGAD_PACKAGE)
            
            appList.add(
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    isCloned = clonedApps.contains(packageName),
                    isNagad = isNagad
                )
            )
        }
        
        // Prioritize Nagad at top
        appList.sortWith(compareByDescending<AppInfo> { it.isNagad }.thenBy { it.appName })
        
        recyclerView.adapter = AppAdapter(appList) { appInfo ->
            if (appInfo.isCloned) {
                launchVirtualApp(appInfo.packageName)
            } else {
                showCloneDialog(appInfo)
            }
        }
    }

    private fun showCloneDialog(appInfo: AppInfo) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            
            val warningText = TextView(context).apply {
                text = if (appInfo.isNagad) {
                    "⚠️ Nagad অ্যাপটি ভার্চুয়াল স্পেসে চালাতে নিচের সেটিংস সঠিক দিন:\n\n• নোটিফিকেশন অন করুন\n• স্টোরেজ পারমিশন দিন\n• Nagad অ্যাকাউন্ট সেটআপ করুন"
                } else {
                    "\"${appInfo.appName}\" ক্লোন করুন?\n\nএটি আলাদা ভার্চুয়াল স্পেসে চলবে।"
                }
                textSize = 14f
                setTextColor(Color.BLACK)
            }
            addView(warningText)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (appInfo.isNagad) "Nagad Clone" else "Clone App")
            .setView(dialogLayout)
            .setPositiveButton("Clone") { _, _ ->
                cloneAppToVirtualSpace(appInfo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun cloneAppToVirtualSpace(appInfo: AppInfo) {
        if (!isVirtualCoreReady) {
            Toast.makeText(this, "Virtual Engine not ready!", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Get APK path
            val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
            val sourceDir = packageInfo.applicationInfo.sourceDir
            
            // Install to Virtual Space (Root-less method)
            val options = InstallOptions.Builder()
                .setEnableCompat(true)
                .build()
            
            val result = VirtualCore.get().installPackage(sourceDir, options)
            
            if (result.isSuccess) {
                clonedApps.add(appInfo.packageName)
                
                // Save to preferences
                getSharedPreferences("cloned_apps", MODE_PRIVATE).edit().apply {
                    putStringSet("cloned_list", clonedApps)
                    apply()
                }
                
                val message = if (appInfo.isNagad) {
                    "Nagad ভার্চুয়াল স্পেসে ক্লোন হয়েছে!\nএখন ওপেন করুন এবং Nagad চালু করুন।"
                } else {
                    "${appInfo.appName} ক্লোন সম্পূর্ণ!"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                
                appList.find { it.packageName == appInfo.packageName }?.isCloned = true
                recyclerView.adapter?.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Clone failed: ${result.error}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchVirtualApp(packageName: String) {
        if (!isVirtualCoreReady) {
            Toast.makeText(this, "Virtual Engine not ready!", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Launch app inside virtual space
            val intent = VirtualCore.get().launchIntent(packageName)
            if (intent != null) {
                startActivity(intent)
                
                val toastMessage = if (packageName == NAGAD_PACKAGE) {
                    "Nagad ভার্চুয়াল স্পেসে চলছে!"
                } else {
                    "ভার্চুয়াল স্পেসে চালু হচ্ছে..."
                }
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot launch $packageName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Launch failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // RecyclerView Adapter
    inner class AppAdapter(
        private val apps: List<AppInfo>,
        private val onItemClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(30, 20, 30, 20)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(Color.parseColor("#2a2a2a"))
            }
            
            val iconView = ImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(60, 60)
                setPadding(0, 0, 20, 0)
            }
            layout.addView(iconView)
            
            val textContainer = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val nameView = TextView(parent.context).apply {
                textSize = 16f
                setTextColor(Color.WHITE)
            }
            textContainer.addView(nameView)
            
            val statusView = TextView(parent.context).apply {
                textSize = 11f
                setTextColor(Color.parseColor("#888888"))
            }
            textContainer.addView(statusView)
            
            layout.addView(textContainer)
            
            val buttonView = TextView(parent.context).apply {
                textSize = 22f
                setTextColor(Color.parseColor("#FF6B00")) // Nagad color for buttons
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(80, 80)
            }
            layout.addView(buttonView)
            
            return ViewHolder(layout, iconView, nameView, statusView, buttonView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.nameView.text = app.appName
            holder.iconView.setImageDrawable(app.icon)
            
            // Special styling for Nagad
            if (app.isNagad) {
                holder.nameView.setTextColor(Color.parseColor("#FF6B00"))
                holder.nameView.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.statusView.text = if (app.isCloned) "✓ Nagad Cloned (Virtual Space)" else "★ Nagad - Clone for Dual Account"
            } else {
                holder.statusView.text = if (app.isCloned) "✓ Cloned" else "Tap + to clone"
            }
            
            holder.buttonView.text = if (app.isCloned) "▶" else "+"
            
            holder.itemView.setOnClickListener {
                onItemClick(app)
            }
        }

        override fun getItemCount() = apps.size

        inner class ViewHolder(
            itemView: android.view.View,
            val iconView: ImageView,
            val nameView: TextView,
            val statusView: TextView,
            val buttonView: TextView
        ) : RecyclerView.ViewHolder(itemView)
    }

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        var isCloned: Boolean,
        val isNagad: Boolean = false
    )
}

// CardView import
import androidx.cardview.widget.CardView
import android.view.ViewGroup
import android.widget.ImageView
