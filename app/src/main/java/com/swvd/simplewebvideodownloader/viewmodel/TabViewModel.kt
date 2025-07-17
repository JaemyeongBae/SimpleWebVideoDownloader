package com.swvd.simplewebvideodownloader.viewmodel

import androidx.lifecycle.ViewModel
import com.swvd.simplewebvideodownloader.models.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 탭 관리 뷰모델
 * 다중 탭의 생성, 전환, 닫기 기능을 담당
 */
class TabViewModel : ViewModel() {
    
    // 탭 목록 상태
    private val _tabs = MutableStateFlow(listOf(Tab()))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    
    // 현재 선택된 탭 인덱스
    private val _currentTabIndex = MutableStateFlow(0)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()
    
    // 탭 오버뷰 표시 상태
    private val _showTabOverview = MutableStateFlow(false)
    val showTabOverview: StateFlow<Boolean> = _showTabOverview.asStateFlow()
    
    // 현재 선택된 탭 정보
    val currentTab: Tab?
        get() {
            val tabList = _tabs.value
            val index = _currentTabIndex.value
            return if (tabList.isNotEmpty() && index in tabList.indices) {
                tabList[index]
            } else null
        }
    
    /**
     * 새 탭 추가
     */
    fun addNewTab(url: String = "", title: String = "새 탭") {
        val newTab = Tab(url = url, title = title)
        val updatedTabs = _tabs.value + newTab
        _tabs.value = updatedTabs
        _currentTabIndex.value = updatedTabs.size - 1
    }
    
    /**
     * 탭 닫기
     */
    fun closeTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        
        // 최소 1개 탭 유지
        if (currentTabs.size <= 1) {
            return
        }
        
        // 유효한 인덱스 확인
        if (index !in currentTabs.indices) {
            return
        }
        
        currentTabs.removeAt(index)
        _tabs.value = currentTabs
        
        // 현재 탭 인덱스 조정
        val currentIndex = _currentTabIndex.value
        when {
            index < currentIndex -> {
                _currentTabIndex.value = currentIndex - 1
            }
            index == currentIndex && currentIndex >= currentTabs.size -> {
                _currentTabIndex.value = currentTabs.size - 1
            }
        }
    }
    
    /**
     * 탭 전환
     */
    fun switchTab(index: Int) {
        val currentTabs = _tabs.value
        if (index in currentTabs.indices) {
            _currentTabIndex.value = index
        }
    }
    
    /**
     * 탭 정보 업데이트
     */
    fun updateTab(index: Int, updatedTab: Tab) {
        val currentTabs = _tabs.value.toMutableList()
        if (index in currentTabs.indices) {
            currentTabs[index] = updatedTab
            _tabs.value = currentTabs
        }
    }
    
    /**
     * 현재 탭 URL 업데이트
     */
    fun updateCurrentTabUrl(url: String) {
        val currentIndex = _currentTabIndex.value
        val currentTabs = _tabs.value
        
        if (currentIndex in currentTabs.indices) {
            val updatedTab = currentTabs[currentIndex].copy(url = url)
            updateTab(currentIndex, updatedTab)
        }
    }
    
    /**
     * 현재 탭 제목 업데이트
     */
    fun updateCurrentTabTitle(title: String) {
        val currentIndex = _currentTabIndex.value
        val currentTabs = _tabs.value
        
        if (currentIndex in currentTabs.indices) {
            val truncatedTitle = title.take(15)
            val updatedTab = currentTabs[currentIndex].copy(title = truncatedTitle)
            updateTab(currentIndex, updatedTab)
        }
    }
    
    /**
     * 탭 오버뷰 표시/숨김
     */
    fun toggleTabOverview() {
        _showTabOverview.value = !_showTabOverview.value
    }
    
    fun showTabOverview() {
        _showTabOverview.value = true
    }
    
    fun hideTabOverview() {
        _showTabOverview.value = false
    }
    
    /**
     * 모든 탭 닫기 (새 탭 하나 생성)
     */
    fun closeAllTabs() {
        _tabs.value = listOf(Tab())
        _currentTabIndex.value = 0
        _showTabOverview.value = false
    }
    
    /**
     * 다른 탭들 닫기 (현재 탭만 유지)
     */
    fun closeOtherTabs() {
        val currentTab = currentTab
        if (currentTab != null) {
            _tabs.value = listOf(currentTab)
            _currentTabIndex.value = 0
            _showTabOverview.value = false
        }
    }
    
    /**
     * 탭 개수 조회
     */
    val tabCount: Int
        get() = _tabs.value.size
    
    /**
     * 빈 탭 여부 확인
     */
    fun isCurrentTabEmpty(): Boolean {
        return currentTab?.url?.isEmpty() == true
    }
    
    /**
     * 탭 복제
     */
    fun duplicateTab(index: Int) {
        val currentTabs = _tabs.value
        if (index in currentTabs.indices) {
            val tabToDuplicate = currentTabs[index]
            val duplicatedTab = Tab(
                url = tabToDuplicate.url,
                title = "${tabToDuplicate.title} (복사)"
            )
            val updatedTabs = currentTabs.toMutableList()
            updatedTabs.add(index + 1, duplicatedTab)
            _tabs.value = updatedTabs
            _currentTabIndex.value = index + 1
        }
    }
    
    /**
     * 탭 순서 변경
     */
    fun moveTab(fromIndex: Int, toIndex: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (fromIndex in currentTabs.indices && toIndex in currentTabs.indices) {
            val tab = currentTabs.removeAt(fromIndex)
            currentTabs.add(toIndex, tab)
            _tabs.value = currentTabs
            
            // 현재 탭 인덱스 조정
            val currentIndex = _currentTabIndex.value
            when (currentIndex) {
                fromIndex -> _currentTabIndex.value = toIndex
                in (minOf(fromIndex, toIndex) until maxOf(fromIndex, toIndex)) -> {
                    _currentTabIndex.value = if (fromIndex < toIndex) {
                        currentIndex - 1
                    } else {
                        currentIndex + 1
                    }
                }
            }
        }
    }
}