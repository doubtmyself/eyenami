package team.eyenami.ui.setting

import android.os.Bundle
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import team.eyenami.R
import team.eyenami.databinding.FragmentQueryBinding

class SettingFragment : Fragment(R.layout.fragment_setting) {

    private val binding by viewBinding(FragmentQueryBinding::bind)

    // This property is only valid between onCreateView and
    // onDestroyView.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}