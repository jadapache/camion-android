package com.pronavicola.camion.presentation.registros

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity

class VariableRegistroPagingSource(
    private val dao: VariableRegistroDao,
    private val startDate: String? = null,
    private val endDate: String? = null,
    private val plateFilter: String? = null,
    private val statusFilter: String? = null,
) : PagingSource<Int, VariableRegistroEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VariableRegistroEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            val data = dao.getPagedRecords(
                limit = pageSize,
                offset = page * pageSize,
                startDate = startDate,
                endDate = endDate,
                plate = plateFilter,
                status = statusFilter,
            )

            LoadResult.Page(
                data = data,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (data.isEmpty() || data.size < pageSize) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VariableRegistroEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
