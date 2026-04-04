import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/home',
    children: [
      { path: '/home', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: '工作台' } },
      { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { title: '个人资料' } },
      { path: '/notifications', name: 'Notifications', component: () => import('../views/Notifications.vue'), meta: { title: '通知中心' } },
      { path: '/admin/command-center', name: 'AdminCommandCenter', component: () => import('../views/admin/AdminCommandCenter.vue'), meta: { title: '系统监控', role: 'ADMIN' } },
      { path: '/admin/users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue'), meta: { title: '用户管理', role: 'ADMIN' } },
      { path: '/admin/notices', name: 'NoticeManage', component: () => import('../views/admin/NoticeManage.vue'), meta: { title: '通知公告', role: 'ADMIN' } },
      { path: '/admin/feedbacks', name: 'FeedbackManage', component: () => import('../views/admin/FeedbackManage.vue'), meta: { title: '问题反馈', role: 'ADMIN' } },
      { path: '/admin/settings', name: 'SystemSettings', component: () => import('../views/admin/SystemSettings.vue'), meta: { title: '系统设置', role: 'ADMIN' } },
      { path: '/teacher/classes', name: 'ClassManage', component: () => import('../views/teacher/ClassManage.vue'), meta: { title: '班级管理', role: 'TEACHER' } },
      { path: '/teacher/classes/:classId/students', name: 'ClassStudentList', component: () => import('../views/teacher/ClassStudentList.vue'), meta: { title: '班级学生', role: 'TEACHER' } },
      { path: '/teacher/assignments', name: 'TeacherAssignmentOverview', component: () => import('../views/teacher/TeacherAssignmentOverview.vue'), meta: { title: '作业总览', role: 'TEACHER' } },
      { path: '/teacher/assignments/create', name: 'TeacherAssignmentCreate', component: () => import('../views/teacher/TeacherAssignmentEditor.vue'), meta: { title: '发布作业', role: 'TEACHER' } },
      { path: '/teacher/assignments/:assignmentId/settings', name: 'TeacherAssignmentSettings', component: () => import('../views/teacher/TeacherAssignmentEditor.vue'), meta: { title: '作业设置', role: 'TEACHER' } },
      { path: '/teacher/assignments/submissions', redirect: '/teacher/assignments' },
      { path: '/teacher/assignments/:assignmentId/submissions', redirect: '/teacher/assignments' },
      { path: '/teacher/assignments/plagiarism', redirect: '/teacher/assignments/plagiarism/results' },
      { path: '/teacher/assignments/:assignmentId/plagiarism', redirect: to => `/teacher/assignments/${to.params.assignmentId}/plagiarism/results` },
      { path: '/teacher/assignments/plagiarism/run', name: 'TeacherAssignmentPlagiarismLaunchEntry', component: () => import('../views/teacher/AssignmentsPlagiarismPage.vue'), meta: { title: '发起查重', role: 'TEACHER' } },
      { path: '/teacher/assignments/:assignmentId/plagiarism/run', name: 'TeacherAssignmentPlagiarismLaunch', component: () => import('../views/teacher/AssignmentsPlagiarismPage.vue'), meta: { title: '发起查重', role: 'TEACHER' } },
      { path: '/teacher/assignments/plagiarism/results', name: 'TeacherAssignmentPlagiarismEntry', component: () => import('../views/teacher/TeacherAssignmentPlagiarism.vue'), meta: { title: '查看结果', role: 'TEACHER' } },
      { path: '/teacher/assignments/:assignmentId/plagiarism/results', name: 'TeacherAssignmentPlagiarism', component: () => import('../views/teacher/TeacherAssignmentPlagiarism.vue'), meta: { title: '查看结果', role: 'TEACHER' } },
      { path: '/teacher/assignments/:assignmentId', name: 'AssignmentDetail', component: () => import('../views/teacher/AssignmentDetail.vue'), meta: { title: '作业详情', role: 'TEACHER' } },
      { path: '/teacher/submissions/:submissionId', redirect: '/teacher/assignments' },
      { path: '/teacher/similarity-pairs/:pairId', name: 'SimilarityPairDetail', component: () => import('../views/teacher/SimilarityPairDetail.vue'), meta: { title: '相似对详情', role: 'TEACHER' } },
      { path: '/teacher/similarity-pairs/:pairId/summary', name: 'SimilarityPairSummary', component: () => import('../views/teacher/SimilarityPairSummary.vue'), meta: { title: '处理总结', role: 'TEACHER' } },
      { path: '/teacher/students', name: 'StudentManage', component: () => import('../views/teacher/StudentManage.vue'), meta: { title: '学生管理', role: 'TEACHER' } },
      { path: '/student/tasks', name: 'TaskList', component: () => import('../views/student/TaskList.vue'), meta: { title: '我的作业', role: 'STUDENT' } },
      { path: '/student/classes', name: 'MyClass', component: () => import('../views/student/MyClass.vue'), meta: { title: '我的班级', role: 'STUDENT' } },
      { path: '/student/assignments/:assignmentId', name: 'StudentAssignmentDetail', component: () => import('../views/student/AssignmentDetail.vue'), meta: { title: '作业详情', role: 'STUDENT' } },
      { path: '/student/assignments/:assignmentId/submit', name: 'StudentAssignmentSubmit', component: () => import('../views/student/AssignmentSubmit.vue'), meta: { title: '提交作业', role: 'STUDENT' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const homeByRole = {
  ADMIN: '/admin/command-center',
  TEACHER: '/home',
  STUDENT: '/student/tasks'
}

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const hasUser = Boolean(user?.role)

  if (hasUser && (to.path === '/login' || to.path === '/register')) {
    next(homeByRole[user.role] || '/home')
    return
  }

  if (!to.meta.public && !hasUser) {
    next('/login')
    return
  }

  if (to.path === '/home' && hasUser && user.role !== 'TEACHER') {
    next(homeByRole[user.role] || '/home')
    return
  }

  if (to.meta.role && to.meta.role !== user.role) {
    next(homeByRole[user.role] || '/home')
    return
  }

  next()
})

export default router


