<template>
    <div class="student-manage">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>学生管理</span>
                    <div class="header-actions">
                        <!-- 搜索和筛选区域 -->
                        <div class="filter-box">
                            <el-input 
                                v-model="searchKeyword" 
                                placeholder="搜索姓名或学号..." 
                                prefix-icon="Search"
                                clearable
                                @clear="handleSearch"
                                @keyup.enter="handleSearch"
                                style="width: 200px"
                            />
                            
                            <el-select 
                                v-model="selectedCollege" 
                                placeholder="学院" 
                                clearable 
                                @change="handleCollegeChange"
                                style="width: 150px"
                            >
                                <el-option 
                                    v-for="item in collegeOptions" 
                                    :key="item" 
                                    :label="item" 
                                    :value="item" 
                                />
                            </el-select>

                            <el-select 
                                v-model="selectedClass" 
                                placeholder="班级" 
                                clearable 
                                @change="handleSearch"
                                style="width: 150px"
                            >
                                <el-option 
                                    v-for="item in classOptions" 
                                    :key="item" 
                                    :label="item" 
                                    :value="item" 
                                />
                            </el-select>
                        </div>
                        
                        <div class="right-actions">
                            <el-button icon="Filter" circle />
                            <el-button icon="Download" circle />
                        </div>
                    </div>
                </div>
            </template>

            <el-table :data="studentList" style="width: 100%" v-loading="loading">
                <el-table-column prop="student_number" label="学号" width="120" sortable />
                <el-table-column prop="nickname" label="姓名" width="100" />
                <el-table-column prop="class_name" label="班级" width="120" />
                
                <!-- 模拟数据列 -->
                <el-table-column label="已交作业" width="100" align="center">
                    <template #default="scope">
                        {{ Math.floor(Math.random() * 10) }} / 10
                    </template>
                </el-table-column>
                
                <el-table-column label="最后提交" width="180">
                    <template #default="scope">
                        2024-03-{{ 10 + Math.floor(Math.random() * 20) }} 14:30
                    </template>
                </el-table-column>

                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default>
                        <el-tag type="success" size="small">在读</el-tag>
                    </template>
                </el-table-column>

                <el-table-column label="操作" min-width="150" align="center" fixed="right">
                    <template #default="scope">
                        <el-button type="primary" link size="small">查看详情</el-button>
                        <el-button type="primary" link size="small">作业记录</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination
                    :current-page="currentPage"
                    :page-size="pageSize"
                    :total="total"
                    layout="total, prev, pager, next"
                    @current-change="handleCurrentChange"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { Search, Sort, Filter, Download } from '@element-plus/icons-vue'

const studentList = ref([])
const collegeOptions = ref([])
const classOptions = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const selectedCollege = ref('')
const selectedClass = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const handleSearch = () => {
    fetchStudents()
}

const handleCollegeChange = () => {
    selectedClass.value = ''
    fetchClasses(selectedCollege.value)
    handleSearch()
}

const fetchColleges = async () => {
    try {
        const res = await request.get('/student/colleges')
        collegeOptions.value = res || []
    } catch (e) {
        console.error('获取学院列表失败', e)
    }
}

const fetchClasses = async (college) => {
    try {
        const res = await request.get('/student/class-options', { params: { college } })
        classOptions.value = res || []
    } catch (e) {
        console.error('获取班级列表失败', e)
    }
}

const fetchStudents = async () => {
    loading.value = true
    try {
        const res = await request.get('/student/list', {
            params: {
                keyword: searchKeyword.value,
                college: selectedCollege.value,
                className: selectedClass.value
            }
        })
        studentList.value = res || []
        total.value = res ? res.length : 0 // 暂时用list长度，后端没做分页
    } catch (e) {
        console.error('获取学生列表失败', e)
    } finally {
        loading.value = false
    }
}

const handleCurrentChange = (val) => {
    currentPage.value = val
    // 前端假分页或者后端加分页
}

onMounted(() => {
    fetchColleges()
    fetchClasses()
    fetchStudents()
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
    justify-content: space-between;
    align-items: center;
    width: 100%;
}

.filter-box {
    display: flex;
    gap: 12px;
    align-items: center;
}

.right-actions {
    display: flex;
    gap: 8px;
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}
</style>
