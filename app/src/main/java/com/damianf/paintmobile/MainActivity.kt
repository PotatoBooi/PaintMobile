package com.damianf.paintmobile


import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import com.damianf.paintmobile.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.damianf.paintmobile.utils.DEFAULT_STROKE
import com.flask.colorpicker.builder.ColorPickerClickListener
import com.flask.colorpicker.OnColorSelectedListener
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.brush_dialog.*
import kotlinx.android.synthetic.main.brush_dialog.view.*
import kotlinx.android.synthetic.main.save_file_dialog.*
import kotlinx.android.synthetic.main.save_file_dialog.view.*
import java.io.File
import java.io.FileOutputStream
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        bindUI()

    }

    private fun bindUI() {
        viewModel.drawing.observe(this, Observer {
            draw_view.mPaths = it
        })
        viewModel.stroke.observe(this, Observer {
            draw_view.setStrokeWidth(it)
        })
        viewModel.color.observe(this, Observer {
            draw_view.setColor(it)
        })
        fab_colors.setOnClickListener {
            showColorPicker()
        }
        fab_brush.setOnClickListener {
            showBrushDialog()
        }
        fab_save.setOnClickListener {
            saveImage()
        }
        fab_undo.setOnClickListener {
            draw_view.undo()
        }
        fab_redo.setOnClickListener {
            draw_view.redo()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.savePaths(draw_view.mPaths)
    }

    private fun showColorPicker() {
        val currentColor = viewModel.color.value ?: getColor(R.color.colorPrimary)
        Toast.makeText(this, currentColor.toString(), Toast.LENGTH_SHORT).show()
        ColorPickerDialogBuilder
            .with(this@MainActivity)
            .setTitle("Choose color")
  //          .initialColor(currentColor)
//            .alphaSliderOnly()
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)

            .density(12)
            .setOnColorSelectedListener { selectedColor ->
                Toast.makeText(this, selectedColor.toString(), Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(
                "ok"
            ) { dialog, selectedColor, allColors -> viewModel.color.postValue(selectedColor) }
            .setNegativeButton("cancel") { dialog, which -> }
            .build()
            .show()
    }

    private fun showBrushDialog() {
        val builder = AlertDialog.Builder(this)
            .create()
        val view = layoutInflater.inflate(R.layout.brush_dialog, findViewById(android.R.id.content), false)
            .apply {
                seekBarStroke.max = 255
                seekBarStroke.progress = viewModel.stroke.value?.toInt() ?: DEFAULT_STROKE.toInt()
                txtStrokeValue.text = seekBarStroke.progress.toString()
                seekBarStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        txtStrokeValue.text = seekBar?.progress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })

                buttonCancel.setOnClickListener {
                    builder.dismiss()
                }
                buttonOK.setOnClickListener {
                    viewModel.stroke.postValue(seekBarStroke.progress.toFloat())
                    builder.dismiss()
                }
            }
        builder.apply {
            setTitle("Set stroke width")
            setView(view)
            show()
        }
    }

    private fun showSaveImageDialog() {
        val view = layoutInflater.inflate(R.layout.save_file_dialog, findViewById(android.R.id.content), false)
            .apply {
                val fileName =
                    "drawing${SimpleDateFormat("MMddyyyyHHmmss").format(Calendar.getInstance().time)}".trim()
                editTextFilename.setSelectAllOnFocus(true)
                editTextFilename.setText(fileName)
            }
        val builder = AlertDialog.Builder(this)
            .apply {
                setView(view)
                setTitle("Save drawing")
                setPositiveButton("Save") { _, _ ->
                    saveFile(view.editTextFilename.text.toString())
                }
                setNegativeButton("Cancel") { _, _ -> {} }
            }
        val dialog = builder.create()
            .apply { window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE) }
            .show()
    }

    private fun saveFile(fileName: String) {
        val fileDir = "${Environment.DIRECTORY_PICTURES}/PaintMobile/"
        val path = Environment.getExternalStoragePublicDirectory(fileDir)
        val file = File(path, "$fileName.png")
        path.mkdirs()
        file.createNewFile()
        val outputStream = FileOutputStream(file)
        val bitmap = draw_view.getBitmap()
            .apply {
                compress(Bitmap.CompressFormat.PNG,100,outputStream)
            }
        outputStream.flush()
        outputStream.close()
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null,null)
    }

    private fun saveImage() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    showSaveImageDialog()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
                }

            }).check()
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this.applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
