<template>
    <div class="class-manage">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>班级管理</span>
                    <div class="header-actions">
                        <el-badge :value="applicationCount" :hidden="applicationCount === 0" class="item">
                            <el-button type="warning" @click="handleOpenApplications">入班申请</el-button>
                        </el-badge>
                        <button class="button" type="button" @click="dialogVisible = true">
                            <span class="button__text">新建班级</span>
                            <span class="button__icon">
                                <svg xmlns="http://www.w3.org/2000/svg" width="24" viewBox="0 0 24 24" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" stroke="currentColor" height="24" fill="none" class="svg">
                                    <line y2="19" y1="5" x2="12" x1="12"></line>
                                    <line y2="12" y1="12" x2="19" x1="5"></line>
                                </svg>
                            </span>
                        </button>
                    </div>
                </div>
            </template>

            <el-table :data="classList" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="100" />
                <el-table-column prop="className" label="班级名称" min-width="150">
                    <template #default="scope">
                        <span class="class-name-link" @click="handleManageStudents(scope.row)">
                            {{ scope.row.className }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column prop="courseName" label="课程名称" min-width="150" />
                <el-table-column prop="inviteCode" label="邀请码" width="180">
                    <template #default="scope">
                        <div class="invite-code-wrapper">
                            <el-tag type="info" class="code-tag">{{ scope.row.inviteCode }}</el-tag>
                            <el-tooltip content="点击复制" placement="top">
                                <el-icon class="copy-icon" @click="copyCode(scope.row.inviteCode)">
                                    <DocumentCopy />
                                </el-icon>
                            </el-tooltip>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="studentCount" label="学生人数" width="120" align="center" />
                <el-table-column prop="createTime" label="创建时间" width="180">
                    <template #default="scope">
                        {{ new Date(scope.row.createTime).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                    <template #default="scope">
                        <el-tooltip content="删除班级" placement="top">
                            <el-button 
                                type="danger" 
                                icon="Delete" 
                                circle 
                                @click="handleDelete(scope.row)"
                            />
                        </el-tooltip>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination
                    :current-page="currentPage" @update:current-page="currentPage = $event"
                    :page-size="pageSize" @update:page-size="pageSize = $event"
                    :page-sizes="[10, 20, 50, 100]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                />
            </div>
        </el-card>

        <!-- 新建班级弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            title="新建班级"
            width="400px"
            @close="resetForm"
        >
            <el-form :model="form" ref="formRef" :rules="rules" label-width="80px">
                <el-form-item label="班级名称" prop="className">
                    <el-input v-model="form.className" placeholder="请输入班级名称" />
                </el-form-item>
                <el-form-item label="课程名称" prop="courseName">
                    <el-input v-model="form.courseName" placeholder="请输入课程名称" @keyup.enter="handleCreate" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleCreate" :loading="createLoading">
                        确定
                    </el-button>
                </span>
            </template>
        </el-dialog>
        <!-- 入班申请弹窗 -->
        <el-dialog
            v-model="applicationDialogVisible"
            title="入班申请处理"
            width="1200px"
        >
            <el-table :data="applicationList" v-loading="applicationLoading" style="width: 100%" height="400">
                <el-table-column prop="school" label="学校" width="120" show-overflow-tooltip />
                <el-table-column prop="studentNumber" label="学号" width="120" sortable />
                <el-table-column prop="nickname" label="姓名" width="100">
                    <template #default="scope">
                        {{ scope.row.nickname || scope.row.username }}
                    </template>
                </el-table-column>
                <el-table-column prop="college" label="系/学院" width="120" show-overflow-tooltip />
                <el-table-column prop="adminClass" label="行政班级" width="120" show-overflow-tooltip />
                <el-table-column prop="className" label="申请加入班级" min-width="150" />
                <el-table-column prop="applyTime" label="申请时间" width="180">
                    <template #default="scope">
                        {{ formatDate(scope.row.applyTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="150" align="center" fixed="right">
                    <template #default="scope">
                        <el-button 
                            type="success" 
                            size="small" 
                            @click="handleApprove(scope.row)"
                        >通过</el-button>
                        <el-button 
                            type="danger" 
                            size="small" 
                            @click="handleReject(scope.row)"
                        >拒绝</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="applicationDialogVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import useClipboard from 'vue-clipboard3'

const router = useRouter()
const { toClipboard } = useClipboard()

const classList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const createLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)

// 入班申请相关
const applicationDialogVisible = ref(false)
const applicationList = ref([])
const applicationLoading = ref(false)
const applicationCount = ref(0) // 待处理数量

const form = reactive({
    className: '',
    courseName: ''
})

const rules = {
    className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
    courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

const fetchClasses = async () => {
    loading.value = true
    try {
        const res = await request.get('/teacher/classes', {
            params: {
                page: currentPage.value,
                limit: pageSize.value
            }
        })
        classList.value = res.records
        total.value = res.total
        
        // 顺便获取一下待处理申请数量（如果后端支持，这里可以单独调用，或者在list中返回）
        // 暂时单独调用一下获取列表来计数（虽然有点浪费，但先这样）
        fetchApplications(true)
    } catch (e) {
        console.error('获取班级列表失败', e)
    } finally {
        loading.value = false
    }
}

const handleOpenApplications = () => {
    applicationDialogVisible.value = true
    fetchApplications()
}

const fetchApplications = async (countOnly = false) => {
    if (!countOnly) applicationLoading.value = true
    try {
        const res = await request.get('/teacher/classes/applications')
        if (countOnly) {
            applicationCount.value = res ? res.length : 0
        } else {
            applicationList.value = res || []
            applicationCount.value = res ? res.length : 0
        }
    } catch (e) {
        console.error('获取申请列表失败', e)
    } finally {
        if (!countOnly) applicationLoading.value = false
    }
}

const handleApprove = async (row) => {
    try {
        await request.post(`/teacher/classes/applications/${row.id}/approve`)
        ElMessage.success('已通过')
        fetchApplications() // 刷新申请列表
        fetchClasses() // 刷新班级列表（更新人数）
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleReject = async (row) => {
    try {
        await request.post(`/teacher/classes/applications/${row.id}/reject`)
        ElMessage.success('已拒绝')
        fetchApplications()
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleSizeChange = (val) => {
    pageSize.value = val
    fetchClasses()
}

const handleCurrentChange = (val) => {
    currentPage.value = val
    fetchClasses()
}

// 打开学生管理
const handleManageStudents = (row) => {
    router.push(`/teacher/classes/${row.id}/students`)
}

const handleDelete = (row) => {
    ElMessageBox.confirm(
        `确定要删除班级 "${row.className}" 吗？此操作不可恢复。`,
        '警告',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            await request.delete(`/teacher/classes/${row.id}`)
            ElMessage.success('删除成功')
            fetchClasses() // 刷新列表
        } catch (e) {
            console.error('删除失败', e)
            ElMessage.error('删除失败')
        }
    }).catch(() => {
        // 取消删除
    })
}

// 创建班级
const handleCreate = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
        if (valid) {
            createLoading.value = true
            try {
                // 调用后端接口
                const res = await request.post('/teacher/classes/create', {
                    className: form.className,
                    courseName: form.courseName
                })
                
                // 关闭弹窗
                dialogVisible.value = false
                
                // 显示带邀请码的通知
                ElNotification({
                    title: '创建成功',
                    message: `班级已创建！邀请码为：${res.inviteCode || '未知'}`,
                    type: 'success',
                    duration: 0 // 不自动关闭，防止用户没看清
                })
                
                // 刷新列表
                fetchClasses()
            } catch (e) {
                console.error(e)
            } finally {
                createLoading.value = false
            }
        }
    })
}

// 复制邀请码
const copyCode = async (code) => {
    try {
        await toClipboard(code)
        ElMessage.success('邀请码已复制到剪贴板')
    } catch (e) {
        ElMessage.error('复制失败')
    }
}

const resetForm = () => {
    if (formRef.value) {
        formRef.value.resetFields()
    }
}

const formatDate = (timeStr) => {
    if (!timeStr) return ''
    return new Date(timeStr).toLocaleString()
}

onMounted(() => {
    fetchClasses()
})
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.header-actions {
    display: flex;
    gap: 16px;
    align-items: center;
}
.class-name-link {
    color: #409EFF;
    cursor: pointer;
    font-weight: 500;
    transition: color 0.3s;
}
.class-name-link:hover {
    color: #66b1ff;
    text-decoration: underline;
}
.code-tag {
    font-family: monospace;
    font-size: 14px;
    letter-spacing: 1px;
}
.invite-code-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
}
.copy-icon {
    cursor: pointer;
    color: #409EFF;
    font-size: 16px;
    transition: color 0.3s;
}
.copy-icon:hover {
    color: #66b1ff;
}
.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

/* Custom Create Class Button */
.button {
  position: relative;
  width: 150px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  border: 1px solid #34974d;
  background-color: #3aa856;
}

.button, .button__icon, .button__text {
  transition: all 0.3s;
}

.button .button__text {
  transform: translateX(30px);
  color: #fff;
  font-weight: 600;
}

.button .button__icon {
  position: absolute;
  transform: translateX(109px);
  height: 100%;
  width: 39px;
  background-color: #34974d;
  display: flex;
  align-items: center;
  justify-content: center;
}

.button .svg {
  width: 30px;
  stroke: #fff;
}

.button:hover {
  background: #34974d;
}

.button:hover .button__text {
  color: transparent;
}

.button:hover .button__icon {
  width: 148px;
  transform: translateX(0);
}

.button:active .button__icon {
  background-color: #2e8644;
}

.button:active {
  border: 1px solid #2e8644;
}
</style>