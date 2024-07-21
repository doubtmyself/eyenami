//package team.eyenami.ui.settingFragment
//
//import android.os.Bundle
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewConfiguration
//import android.widget.NumberPicker
//import androidx.appcompat.widget.SwitchCompat
//import androidx.fragment.app.Fragment
//import by.kirich1409.viewbindingdelegate.viewBinding
//import team.eyenami.R
//import team.eyenami.databinding.FragmentSettingBinding
//import team.eyenami.obj.SettingManager
//import kotlin.math.abs
//
//class SettingFragment2 : Fragment(R.layout.fragment_setting) {
//
//    private val binding by viewBinding(FragmentSettingBinding::bind)
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        initialize()
//        addListener()
//    }
//
//    private fun initialize() {
//        binding.itemVibration.switchCheck.isChecked = SettingManager.getVibrateRun()
//
//        binding.itemDetectionCount.numberPicker.minValue = 5
//        binding.itemDetectionCount.numberPicker.maxValue = 60
//        binding.itemDetectionCount.numberPicker.value =  SettingManager.getDetectionCount()
//    }
//
//    private fun addListener() {
//        binding.itemVibration.root.setOnClickListener {
//            binding.itemVibration.switchCheck.toggle()
//        }
//        binding.itemVibration.switchCheck.setOnClickListener { view ->
//            if (view is SwitchCompat) {
//                SettingManager.setVibrateRun(view.isChecked)
//            }
//        }
//
////        binding.itemDetectionCount.numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
////            if(oldVal == newVal)
////                return@setOnValueChangedListener
////
////            SettingManager.setDetectionCount(newVal)
////        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        SettingManager.setDetectionCount(binding.itemDetectionCount.numberPicker.value)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//    }
//}