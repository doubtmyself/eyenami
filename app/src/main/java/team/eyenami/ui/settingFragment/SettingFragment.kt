package team.eyenami.ui.settingFragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import team.eyenami.R
import team.eyenami.databinding.FragmentSettingBinding
import team.eyenami.obj.SettingManager

class SettingFragment : Fragment(R.layout.fragment_setting) {

    private val binding by viewBinding(FragmentSettingBinding::bind)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.itemVibration.switchTitle.isChecked = SettingManager.getVibrateRun()

        addListener()
    }

    private fun addListener() {
        binding.itemVibration.switchTitle.setOnClickListener { view ->
            if (view is SwitchCompat) {
                SettingManager.setVibrateRun(view.isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}