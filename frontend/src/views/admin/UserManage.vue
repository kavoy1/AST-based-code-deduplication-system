<template>
    <div class="user-manage">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>用户管理</span>
                    <el-button type="primary" @click="fetchUsers">刷新列表</el-button>
                </div>
            </template>
            
            <el-table :data="users" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="100" />
                <el-table-column prop="username" label="用户名" width="150" />
                <el-table-column prop="nickname" label="真实姓名" width="150" />
                <el-table-column prop="email" label="邮箱" width="200" />
                <el-table-column prop="role" label="角色" width="150">
                    <template #default="scope">
                        <el-tag :type="getRoleType(scope.row.role)">
                            {{ formatRole(scope.row.role) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="注册时间" width="180">
                    <template #default="scope">
                        {{ formatDate(scope.row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="scope">
                        <LockSwitch
                            v-model="scope.row.status"
                            :active-value="1"
                            :inactive-value="0"
                            @change="(val) => handleStatusChange(scope.row, val)"
                        />
                    </template>
                </el-table-column>
                <!-- 新增操作列 -->
                <el-table-column label="操作" width="120">
                    <template #default="scope">
                        <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const loading = ref(false)

const fetchUsers = async () => {
    loading.value = true
    try {
        const res = await request.get('/admin/users')
        users.value = res
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const handleStatusChange = async (user, status) => {
    try {
        await request.put('/admin/users/status', null, {
            params: {
                id: user.id,
                status: status
            }
        })
        ElMessage.success('状态更新成功')
    } catch (e) {
        // 如果失败，回滚状态
        user.status = status === 1 ? 0 : 1
    }
}

const handleDelete = (user) => {
    ElMessageBox.confirm(
        '确定要删除该用户吗？此操作不可逆！',
        '警告',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
    .then(async () => {
        try {
            await request.delete(`/admin/users/${user.id}`)
            ElMessage.success('用户删除成功')
            fetchUsers() // 刷新列表
        } catch (e) {
            console.error(e)
        }
    })
    .catch(() => {
        // 取消删除
    })
}

const getRoleType = (role) => {
    switch (role) {
        case 'ADMIN': return 'danger'
        case 'TEACHER': return 'warning'
        case 'STUDENT': return 'success'
        default: return 'info'
    }
}

const formatRole = (role) => {
    switch (role) {
        case 'ADMIN': return '管理员'
        case 'TEACHER': return '教师'
        case 'STUDENT': return '学生'
        default: return role
    }
}

const formatDate = (timeStr) => {
    if (!timeStr) return ''
    return new Date(timeStr).toLocaleString()
}

onMounted(() => {
    fetchUsers()
})
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
</style>