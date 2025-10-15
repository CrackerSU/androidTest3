package com.example.androidtest3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var sampleTextView: TextView
    private lateinit var btnShowDialog: Button

    private val dataList = mutableListOf<ListItem>()
    private lateinit var adapter: MyAdapter

    private var actionMode: ActionMode? = null
    private val selectedItems = mutableSetOf<Int>()

    // 动物图片资源ID数组 - 使用您上传的实际图片
    private val animalImageResources = intArrayOf(
        R.drawable.cat,      // cat.png
        R.drawable.dog,      // dog.jpeg
        R.drawable.elephant, // elephant.jpg
        R.drawable.lion,     // lion.jpeg
        R.drawable.monkey,   // monkey.jpeg
        R.drawable.tiger     // tiger.jpg
    )

    // 动物名称数组
    private val animalNames = arrayOf(
        R.string.animal_cat,
        R.string.animal_dog,
        R.string.animal_elephant,
        R.string.animal_lion,
        R.string.animal_monkey,
        R.string.animal_tiger
    )

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.context_menu, menu)
            mode.title = getString(R.string.action_mode_title, 0)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    // 删除选中的项目（按倒序避免索引问题）
                    selectedItems.sortedDescending().forEach { position ->
                        if (position < dataList.size) {
                            dataList.removeAt(position)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    mode.finish()
                    true
                }
                R.id.action_share -> {
                    // 分享选中的动物
                    val selectedTitles = selectedItems.map {
                        if (it < dataList.size) dataList[it].title else ""
                    }.filter { it.isNotEmpty() }

                    if (selectedTitles.isNotEmpty()) {
                        val shareMessage = "分享动物: ${selectedTitles.joinToString()}"
                        Toast.makeText(this@MainActivity, shareMessage, Toast.LENGTH_LONG).show()

                        // 实际应用中这里可以启动分享Intent
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(shareIntent, "分享动物"))
                    }
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selectedItems.clear()
            actionMode = null
            // 清除选择状态
            listView.clearChoices()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initData()
        setupListView()
        createNotificationChannel()
    }

    private fun initViews() {
        listView = findViewById(R.id.list_view)
        sampleTextView = findViewById(R.id.tv_sample_text)
        btnShowDialog = findViewById(R.id.btn_show_dialog)

        btnShowDialog.setOnClickListener {
            showCustomDialog()
        }
    }

    private fun initData() {
        dataList.clear()

        // 使用实际的6种动物数据
        for (i in animalImageResources.indices) {
            val animalName = getString(animalNames[i])
            dataList.add(
                ListItem(
                    id = i + 1,
                    title = getString(R.string.item_name_format, animalName),
                    description = getString(R.string.item_desc_format, animalName),
                    imageResId = animalImageResources[i]
                )
            )
        }
    }

    private fun setupListView() {
        adapter = MyAdapter(this, dataList)
        listView.adapter = adapter

        // 注册上下文菜单
        registerForContextMenu(listView)

        // 点击事件 - 显示Toast和通知
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (actionMode == null) { // 只有在非多选模式下才响应点击
                val selectedItem = dataList[position]

                // 显示Toast
                Toast.makeText(
                    this,
                    getString(R.string.toast_selected, selectedItem.title),
                    Toast.LENGTH_SHORT
                ).show()

                // 发送通知
                sendNotification(selectedItem.title)

                // 显示动物详情对话框
                showAnimalDetailDialog(selectedItem)
            }
        }

        // 长按启动上下文操作模式
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(object : AbsListView.MultiChoiceModeListener {
            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                if (checked) {
                    selectedItems.add(position)
                } else {
                    selectedItems.remove(position)
                }
                val selectedCount = selectedItems.size
                mode.title = getString(R.string.action_mode_title, selectedCount)

                // 更新ActionMode的菜单项状态（如果需要）
                if (selectedCount > 0) {
                    mode.menu.findItem(R.id.action_delete)?.isEnabled = true
                    mode.menu.findItem(R.id.action_share)?.isEnabled = true
                } else {
                    mode.menu.findItem(R.id.action_delete)?.isEnabled = false
                    mode.menu.findItem(R.id.action_share)?.isEnabled = false
                }
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                actionMode = mode
                mode.menuInflater.inflate(R.menu.context_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return actionModeCallback.onActionItemClicked(mode, item)
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionModeCallback.onDestroyActionMode(mode)
            }
        })
    }

    private fun showCustomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.custom_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, _ ->
                val username = dialogView.findViewById<EditText>(R.id.et_username).text.toString()
                val password = dialogView.findViewById<EditText>(R.id.et_password).text.toString()
                val remember = dialogView.findViewById<CheckBox>(R.id.cb_remember).isChecked

                Toast.makeText(
                    this,
                    "用户名: $username, 记住密码: $remember",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_negative_button)) { dialog, _ ->
                Toast.makeText(this, "点击了取消按钮", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showAnimalDetailDialog(item: ListItem) {
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setMessage(item.description)
            .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun sendNotification(itemTitle: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = System.currentTimeMillis().toInt()
        val channelId = "animal_gallery_channel"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content, itemTitle))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "animal_gallery_channel",
                "动物图库通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于显示动物选择的通知"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_font_small -> {
                sampleTextView.textSize = 12f
                true
            }
            R.id.menu_font_medium -> {
                sampleTextView.textSize = 16f
                true
            }
            R.id.menu_font_large -> {
                sampleTextView.textSize = 20f
                true
            }
            R.id.menu_color_red -> {
                sampleTextView.setTextColor(Color.RED)
                true
            }
            R.id.menu_color_black -> {
                sampleTextView.setTextColor(Color.BLACK)
                true
            }
            R.id.menu_normal -> {
                Toast.makeText(this, "刷新动物列表", Toast.LENGTH_SHORT).show()
                // 可以添加刷新功能
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterForContextMenu(listView)
    }
}