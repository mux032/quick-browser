import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qb.browser.data.SettingsDao
import com.qb.browser.viewmodel.BubbleViewModel

class BubbleViewModelFactory(private val settingsDao: SettingsDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BubbleViewModel(settingsDao) as T
    }
}
