package com.example.mfl

import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel

class Report : Fragment() {
    private lateinit var dbHelper: DbHelper
    private lateinit var email: String
    private lateinit var viewModel: ReportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DbHelper(requireContext())
        email = requireActivity().intent.getStringExtra("userEmail") ?: "unknown@example.com"
        viewModel = ReportViewModel(dbHelper, email)
        viewModel.loadExpensesForPeriod("month")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ReportScreen(viewModel)
            }
        }
    }
}
