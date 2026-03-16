<template>
    <div class="student-class">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>我的班级</span>
                    <el-button type="primary" @click="dialogVisible = true">加入班级</el-button>
                </div>
            </template>

            <div v-if="loading" class="loading-area">
                <el-skeleton :rows="3" animated />
            </div>

            <div v-else-if="classList.length === 0" class="empty-area">
                <el-empty description="暂未加入任何班级" />
            </div>

            <div v-else class="class-grid">
                <el-card v-for="item in classList" :key="item.id" class="class-card" shadow="hover">
                    <div class="class-info">
                        <h3>{{ item.className }}</h3>
                        <p class="course-name">{{ item.courseName }}</p>
                        <div class="meta-info">
                            <span><el-icon><User /></el-icon> {{ item.teacherName }}</span>
                            <span><el-icon><Clock /></el-icon> {{ formatDate(item.joinTime) }}</span>
                        </div>
                    </div>
                    <div class="class-status">
                        <el-tag :type="getStatusType(item.status)">
                            {{ getStatusText(item.status) }}
                        </el-tag>
                    </div>
                </el-card>
            </div>
        </el-card>

        <!-- 加入班级弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            title="加入班级"
            width="400px"
        >
            <el-form :model="form" ref="formRef" :rules="rules">
                <el-form-item prop="inviteCode" label="邀请码">
                    <el-input v-model="form.inviteCode" placeholder="请输入6位邀请码" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleJoin" :loading="joinLoading">
                        确定
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { User, Clock } from '@element-plus/icons-vue'

const classList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const joinLoading = ref(false)
const formRef = ref(null)

const form = reactive({
    inviteCode: ''
})

const rules = {
    inviteCode: [
        { required: true, message: '请输入邀请码', trigger: 'blur' },
        { len: 6, message: '邀请码长度为6位', trigger: 'blur' }
    ]
}

const fetchClasses = async () => {
    loading.value = true
    try {
        const res = await request.get('/student/classes')
        classList.value = res
    } catch (e) {
        console.error('获取班级列表失败', e)
    } finally {
        loading.value = false
    }
}

const handleJoin = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
        if (valid) {
            joinLoading.value = true
            try {
                await request.post('/student/classes/join', null, {
                    params: { inviteCode: form.inviteCode }
                })
                ElMessage.success('申请成功，请等待老师审核')
                dialogVisible.value = false
                form.inviteCode = ''
                fetchClasses() // 刷新列表
            } catch (e) {
                // 错误信息已经在 request.js 拦截器中处理了
            } finally {
                joinLoading.value = false
            }
        }
    })
}

const formatDate = (timeStr) => {
    if (!timeStr) return ''
    return new Date(timeStr).toLocaleDateString()
}

const getStatusType = (status) => {
    return status === 1 ? 'success' : 'warning'
}

const getStatusText = (status) => {
    return status === 1 ? '已加入' : '审核中'
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

.class-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    margin-top: 20px;
}

.class-card {
    position: relative;
    border-radius: 8px;
    transition: transform 0.3s;
}

.class-card:hover {
    transform: translateY(-5px);
}

.class-info h3 {
    margin: 0 0 10px 0;
    font-size: 18px;
    color: #E5EAF3; /* 亮色字体，适应深色背景 */
    font-weight: 600;
}

.course-name {
    color: #A0CFFF; /* 浅蓝色，区分度高 */
    margin-bottom: 15px;
    font-size: 14px;
}

.meta-info {
    display: flex;
    flex-direction: column;
    gap: 8px;
    color: #CFD3DC; /* 浅灰色 */
    font-size: 13px;
}

.meta-info span {
    display: flex;
    align-items: center;
    gap: 5px;
}

.class-status {
    position: absolute;
    top: 15px;
    right: 15px;
}

.loading-area, .empty-area {
    padding: 40px 0;
}
</style>
