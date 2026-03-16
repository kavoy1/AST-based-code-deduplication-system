import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { 
    path: '/login', 
    name: 'Login', 
    component: () => import('../views/Login.vue') 
  },
  { 
    path: '/register', 
    name: 'Register', 
    component: () => import('../views/Register.vue') 
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  { 
    path: '/layout', 
    name: 'Layout', 
    component: () => import('../views/Layout.vue'),
    redirect: '/home',
    children: [
        { path: '/home', name: 'Home', component: () => import('../views/Home.vue') },
        { path: '/admin/users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue') },
        { path: '/admin/notices', name: 'NoticeManage', component: () => import('../views/admin/NoticeManage.vue') },
        { path: '/admin/feedbacks', name: 'FeedbackManage', component: () => import('../views/admin/FeedbackManage.vue') },
        { path: '/notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
        { path: '/teacher/classes', name: 'ClassManage', component: () => import('../views/teacher/ClassManage.vue') },
        { path: '/teacher/classes/:classId/students', name: 'ClassStudentList', component: () => import('../views/teacher/ClassStudentList.vue') },
        { path: '/teacher/students', name: 'StudentManage', component: () => import('../views/teacher/StudentManage.vue') },
        { path: '/student/tasks', name: 'TaskList', component: () => import('../views/student/TaskList.vue') },
        { path: '/student/classes', name: 'MyClass', component: () => import('../views/student/MyClass.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
