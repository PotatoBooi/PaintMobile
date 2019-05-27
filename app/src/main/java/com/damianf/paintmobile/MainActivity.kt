package com.damianf.paintmobile


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.damianf.paintmobile.utils.DEFAULT_STROKE
import com.damianf.paintmobile.viewmodel.MainViewModel
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.nisrulz.sensey.Sensey
import com.github.nisrulz.sensey.ShakeDetector
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.brush_dialog.view.*
import kotlinx.android.synthetic.main.save_file_dialog.view.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Sensey.getInstance().init(this, Sensey.SAMPLING_PERIOD_UI)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        bindUI()
        startShakeListener()

    }

    override fun onDestroy() {
        Sensey.getInstance().stop()
        super.onDestroy()
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
            imv_brush_color.setBackgroundColor(it)
        })
        viewModel.backgroudColor.observe(this, Observer {
            draw_view.setBackgroundColor(it)
            imv_bck_color.setBackgroundColor(it)
        })
        fab_colors.setOnClickListener {
            showColorPicker(isBrushColor = true)
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
        fab_bckrd_color.setOnClickListener {
            showColorPicker(isBrushColor = false)
        }
        fab_gallery.setOnClickListener {
            goToDrawingGallery()
        }
    }

    private fun startShakeListener() {
        val shakeListener = object : ShakeDetector.ShakeListener {
            override fun onShakeDetected() {
                draw_view.undo()
            }

            override fun onShakeStopped() {}
        }
        Sensey.getInstance().startShakeDetection(14.0F,20L,shakeListener)
    }

    override fun onPause() {
        super.onPause()
        viewModel.savePaths(draw_view.mPaths)
    }

    private fun showColorPicker(isBrushColor: Boolean) {

        val dialogTitle = if (isBrushColor) "Choose brush color" else "Choose background color"
        ColorPickerDialogBuilder
            .with(this@MainActivity)
            .setTitle("Choose color")
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)

            .density(12)
            .setPositiveButton(
                "ok"
            ) { _, selectedColor, _ ->
                if (isBrushColor)
                    viewModel.color.postValue(selectedColor)
                else
                    viewModel.backgroudColor.postValue(selectedColor)
            }
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


        val file = File(path, "$fileName.png")
        path.mkdirs()
        file.createNewFile()
        val outputStream = FileOutputStream(file)
        val bitmap = draw_view.getBitmap()
            .apply {
                compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        outputStream.flush()
        outputStream.close()
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)
    }

    private fun goToDrawingGallery() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(
                Uri.parse((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path) + "/PaintMobile"),
                "image/*"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        //startActivity(Intent.createChooser(intent,"Drawing gallery"))
        startActivity(intent)
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

    companion object {
        private val fileDir = "${Environment.DIRECTORY_PICTURES}/PaintMobile/"
        private val path = Environment.getExternalStoragePublicDirectory(fileDir)
    }
}
