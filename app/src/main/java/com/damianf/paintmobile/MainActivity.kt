package com.damianf.paintmobile


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import com.damianf.paintmobile.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import android.content.DialogInterface
import android.widget.Toast
import com.flask.colorpicker.builder.ColorPickerClickListener
import com.flask.colorpicker.OnColorSelectedListener
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder



class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        bindUI()

    }
    private fun bindUI(){
        viewModel.drawing.observe(this, Observer {
            //update draw view

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

        }
        fab_save.setOnClickListener {

        }
        fab_eraser.setOnClickListener {
         //
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.savePaths(draw_view.getBitmap())
    }
    private fun showColorPicker(){
        val currentColor = draw_view.solidColor
        Toast.makeText(this,currentColor.toString(),Toast.LENGTH_SHORT).show()
        ColorPickerDialogBuilder
            .with(this@MainActivity)
            .setTitle("Choose color")
//            .initialColor(currentColor)
//            .alphaSliderOnly()
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setOnColorSelectedListener { selectedColor ->
                Toast.makeText(this,selectedColor.toString(),Toast.LENGTH_SHORT).show()

            }
            .setPositiveButton(
                "ok"
            ) { dialog, selectedColor, allColors -> viewModel.color.postValue(selectedColor) }
            .setNegativeButton("cancel") { dialog, which -> }
            .build()
            .show()
    }

}
