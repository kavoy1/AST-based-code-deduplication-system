<template>
    <div class="class-student-list">
        <el-card>
            <template #header>
                <div class="card-header">
                    <div class="header-left">
                        <el-button link @click="$router.push('/teacher/classes')">
                            <el-icon><ArrowLeft /></el-icon> 返回班级列表
                        </el-button>
                        <span class="title-divider">|</span>
                        <span class="class-title">{{ classInfo.className }} - 学生列表</span>
                    </div>
                    <el-button type="primary" @click="openInviteDialog">邀请学生</el-button>
                </div>
            </template>

            <el-table :data="studentList" style="width: 100%" v-loading="loading">
                <el-table-column prop="student_number" label="学号" width="120" sortable />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="nickname" label="姓名" width="120">
                    <template #default="scope">
                        {{ scope.row.nickname || scope.row.username }}
                    </template>
                </el-table-column>
                <el-table-column prop="admin_class" label="行政班级" width="120" />
                <el-table-column prop="college" label="学院" width="150" />
                <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
                <el-table-column prop="join_time" label="加入时间" width="180" sortable>
                    <template #default="scope">
                        {{ new Date(scope.row.join_time).toLocaleString() }}
                    </template>
                </el-table-column>
                
                <el-table-column label="操作" width="150" align="center" fixed="right">
                    <template #default="scope">
                        <el-popconfirm 
                            title="确定要将该学生移除出班级吗？" 
                            @confirm="handleRemove(scope.row)"
                        >
                            <template #reference>
                                <el-button type="danger" link size="small">移出班级</el-button>
                            </template>
                        </el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 邀请学生弹窗 -->
        <el-dialog v-model="inviteDialogVisible" title="邀请学生加入班级" width="800px">
            <div class="filter-box">
                <el-input 
                    v-model="searchKeyword" 
                    placeholder="搜索姓名或学号..." 
                    prefix-icon="Search"
                    clearable
                    @clear="fetchAllStudents"
                    @keyup.enter="fetchAllStudents"
                    style="width: 200px"
                />
                <el-select 
                    v-model="selectedCollege" 
                    placeholder="学院" 
                    clearable 
                    @change="handleCollegeChange"
                    style="width: 150px"
                >
                    <el-option v-for="item in collegeOptions" :key="item" :label="item" :value="item" />
                </el-select>
                <el-select 
                    v-model="selectedClass" 
                    placeholder="行政班级" 
                    clearable 
                    @change="fetchAllStudents"
                    style="width: 150px"
                >
                    <el-option v-for="item in classOptions" :key="item" :label="item" :value="item" />
                </el-select>
                <el-button type="primary" @click="fetchAllStudents" style="flex-shrink: 0">搜索</el-button>
            </div>

            <el-table :data="allStudents" style="width: 100%; margin-top: 20px;" v-loading="inviteLoading" height="400" border>
                <el-table-column prop="student_number" label="学号" width="150" align="center" />
                <el-table-column prop="nickname" label="姓名" width="150" align="center" />
                <el-table-column prop="class_name" label="行政班级" min-width="150" align="center" />
                <el-table-column label="操作" width="120" align="center" fixed="right">
                    <template #default="scope">
                        <el-button 
                            v-if="isStudentInClass(scope.row.user_id)" 
                            type="info" 
                            plain
                            size="small" 
                            disabled
                            style="width: 80px"
                        >已加入</el-button>
                        <el-button 
                            v-else 
                            type="primary" 
                            plain
                            size="small" 
                            @click="handleInvite(scope.row)"
                            style="width: 80px"
                        >邀请</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const classId = route.params.classId

const classInfo = ref({})
const studentList = ref([])
const loading = ref(false)

// 邀请相关
const inviteDialogVisible = ref(false)
const inviteLoading = ref(false)
const allStudents = ref([])
const searchKeyword = ref('')
const selectedCollege = ref('')
const selectedClass = ref('')
const collegeOptions = ref([])
const classOptions = ref([])

// 获取班级信息
const fetchClassInfo = async () => {
    try {
        const res = await request.get(`/teacher/classes/${classId}`)
        if (res) {
            classInfo.value = res
        }
    } catch (e) {
        console.error(e)
    }
}

// 获取班级学生列表
const fetchClassStudents = async () => {
    loading.value = true
    try {
        const res = await request.get(`/teacher/classes/${classId}/students`)
        studentList.value = res || []
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

// 移除学生
const handleRemove = async (student) => {
    try {
        await request.delete(`/teacher/classes/${classId}/students/${student.id}`)
        ElMessage.success('移除成功')
        fetchClassStudents()
    } catch (e) {
        ElMessage.error('移除失败')
    }
}

// 打开邀请弹窗
const openInviteDialog = () => {
    inviteDialogVisible.value = true
    fetchColleges()
    fetchClasses() // 加载所有行政班级选项
    fetchAllStudents()
}

// 获取所有学生（用于邀请）
const fetchAllStudents = async () => {
    inviteLoading.value = true
    try {
        const res = await request.get('/student/list', {
            params: {
                keyword: searchKeyword.value,
                college: selectedCollege.value,
                className: selectedClass.value
            }
        })
        allStudents.value = res || []
    } catch (e) {
        console.error(e)
    } finally {
        inviteLoading.value = false
    }
}

const fetchColleges = async () => {
    try {
        const res = await request.get('/student/colleges')
        collegeOptions.value = res || []
    } catch (e) {
        console.error(e)
    }
}

const fetchClasses = async (college) => {
    try {
        const res = await request.get('/student/class-options', { params: { college } })
        classOptions.value = res || []
    } catch (e) {
        console.error(e)
    }
}

const handleCollegeChange = () => {
    selectedClass.value = ''
    fetchClasses(selectedCollege.value)
    fetchAllStudents()
}

// 检查学生是否已经在班级中
const isStudentInClass = (studentId) => {
    return studentList.value.some(s => s.id === studentId)
}

// 邀请学生
const handleInvite = async (student) => {
    try {
        await request.post(`/teacher/classes/${classId}/students/invite`, null, {
            params: { studentId: student.user_id }
        })
        ElMessage.success('邀请成功')
        // 刷新列表
        fetchClassStudents()
        // 可以在这里刷新 allStudents 状态，或者直接手动更新 isStudentInClass 的判断依据
        // 简单起见，重新获取班级学生列表即可，allStudents 中的按钮状态依赖 isStudentInClass 计算
    } catch (e) {
        ElMessage.error('邀请失败')
    }
}

onMounted(() => {
    fetchClassInfo()
    fetchClassStudents()
})
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 10px;
}

.title-divider {
    color: #dcdfe6;
}

.class-title {
    font-size: 16px;
    font-weight: bold;
}

.filter-box {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
}
</style>
